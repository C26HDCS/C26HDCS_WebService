package kr.kwater.hdcs.alarm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import kr.kwater.hdcs.alarm.dao.AlarmDAO;
import kr.kwater.hdcs.alarm.service.AlarmStreamService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 알림 개수 SSE 푸시
 *
 * 서버에서 "감시자" 하나만 DB 변경 여부를 확인하고
 * 바뀌었을 때만 접속 중인 사용자에게 각자의 안 읽은 개수를 보낸다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmStreamServiceImpl implements AlarmStreamService {

    /** DB 변경 확인 주기 */
    private static final long POLL_INTERVAL_MS = 1000L;
    /** 연결 유지 신호 주기. 끊어진 연결을 찾고, 프록시가 연결을 닫지 않게 한다. */
    private static final long HEARTBEAT_INTERVAL_MS = 20_000L;
    /** 연결 최대 유지 시간. 지나면 화면이 새 토큰으로 다시 연결한다. (토큰 유효시간 1시간보다 짧게) */
    private static final long EMITTER_TIMEOUT_MS = 10 * 60 * 1000L;
    /** 사용자 1명당 최대 동시 연결 수 (탭 여러 개). 넘으면 가장 오래된 연결을 닫는다. */
    private static final int MAX_CONNECTIONS_PER_USER = 5;

    private final AlarmDAO alarmDAO;

    /** 사용자 ID → 그 사용자의 연결 목록 */
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    private ScheduledExecutorService scheduler;

    /** 마지막으로 확인한 알람 테이블 상태 값 */
    private volatile String lastSignature;
    private final kr.kwater.hdcs.common.DatabaseBackoff retry = new kr.kwater.hdcs.common.DatabaseBackoff();
    @org.springframework.beans.factory.annotation.Value("${hdcs.background.enabled:true}")
    private boolean backgroundEnabled = true;

    @PostConstruct
    public void start() {
        if (!backgroundEnabled) return;
        // 다른 예약 작업에 밀리지 않도록 전용 스레드를 쓴다.
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "alarm-sse");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(this::pollWithBackoff,
                POLL_INTERVAL_MS, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
        scheduler.scheduleWithFixedDelay(() -> safely(this::sendHeartbeat),
                HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        for (List<SseEmitter> list : emitters.values()) {
            for (SseEmitter emitter : list) {
                closeQuietly(emitter);
            }
        }
        emitters.clear();
    }

    @Override
    public SseEmitter subscribe(String userId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> closeQuietly(emitter));
        emitter.onError(e -> remove(userId, emitter));

        emitters.compute(userId, (k, list) -> {
            List<SseEmitter> target = (list == null) ? new CopyOnWriteArrayList<>() : list;
            target.add(emitter);
            while (target.size() > MAX_CONNECTIONS_PER_USER) {
                closeQuietly(target.remove(0));
            }
            return target;
        });

        // 연결되자마자 현재 개수를 한 번 보낸다.
        try {
            send(userId, emitter, "unread", unreadJson(alarmDAO.selectUnreadCount(userId)));
        } catch (Exception e) {
            log.warn("알림 초기 개수 전송 실패: user={}", userId, e);
        }
        return emitter;
    }

    @Override
    public void publishUnreadCount(String userId, int count) {
        List<SseEmitter> list = emitters.get(userId);
        if (list == null) {
            return;
        }
        String json = unreadJson(count);
        for (SseEmitter emitter : list) {
            send(userId, emitter, "unread", json);
        }
    }

    /** 접속자가 있을 때만 DB 상태를 확인하고, 바뀌었으면 접속자 각자의 개수를 보낸다. */
    private void pollChanges() {
        if (emitters.isEmpty()) {
            return;
        }
        String signature = alarmDAO.selectAlarmSignature();
        if (signature == null || signature.equals(lastSignature)) {
            return;
        }
        for (String userId : emitters.keySet()) {
            publishUnreadCount(userId, alarmDAO.selectUnreadCount(userId));
        }
        lastSignature = signature;
    }

    public void pollWithBackoff() {
        if (!retry.acquire()) return;
        try { pollChanges(); retry.success(); }
        catch (RuntimeException ex) {
            log.warn("ALARM_POLL_FAILED reason={} retry_ms={}", ex.getClass().getSimpleName(), retry.failure());
        }
    }

    /** 모든 연결에 주석 한 줄을 보내 살아 있는지 확인한다. 실패한 연결은 정리된다. */
    private void sendHeartbeat() {
        for (Map.Entry<String, List<SseEmitter>> entry : emitters.entrySet()) {
            for (SseEmitter emitter : entry.getValue()) {
                try {
                    synchronized (emitter) {
                        emitter.send(SseEmitter.event().comment("ping"));
                    }
                } catch (Exception e) {
                    remove(entry.getKey(), emitter);
                    closeQuietly(emitter);
                }
            }
        }
    }

    @SuppressWarnings("null")
    private void send(String userId, SseEmitter emitter, String event, String data) {
        try {
            // 여러 스레드(감시자, 요청 처리)가 같은 연결에 동시에 쓰지 않도록 한다.
            synchronized (emitter) {
                emitter.send(SseEmitter.event().name(event).data(data));
            }
        } catch (Exception e) {
            // 브라우저가 이미 연결을 닫은 경우. 목록에서 빼고 닫는다.
            remove(userId, emitter);
            closeQuietly(emitter);
        }
    }

    private void remove(String userId, SseEmitter emitter) {
        emitters.computeIfPresent(userId, (k, list) -> {
            list.remove(emitter);
            return list.isEmpty() ? null : list;
        });
    }

    private static void closeQuietly(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignore) {
            // 이미 닫힌 연결
        }
    }

    private static String unreadJson(int count) {
        return "{\"count\":" + count + "}";
    }

    /** 예약 작업에서 예외가 나도 다음 실행이 취소되지 않도록 감싼다. */
    private void safely(Runnable task) {
        try {
            task.run();
        } catch (Throwable t) {
            log.warn("알림 SSE 작업 오류", t);
        }
    }
}
