package kr.kwater.hdcs.alarm.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import kr.kwater.hdcs.alarm.service.AlarmService;
import kr.kwater.hdcs.alarm.service.AlarmStreamService;
import kr.kwater.hdcs.alarm.vo.AlarmVO;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/alarm")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;
    private final AlarmStreamService alarmStreamService;

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    @GetMapping("/list")
    public ResponseEntity<List<AlarmVO>> getList(AlarmVO vo) throws Exception {
        vo.setCurrentUserId(currentUserId());
        return ResponseEntity.ok(alarmService.getAlarmList(vo));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount() throws Exception {
        int count = alarmService.getUnreadCount(currentUserId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * 헤더 알림 개수 실시간 구독(SSE). 연결을 열어 두면 알람이 바뀔 때만 개수를 보내 준다.
     * 인증 토큰은 일반 API처럼 Authorization 헤더로 받는다. (화면은 fetch 로 연결)
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no"); // nginx 등 프록시가 응답을 모아서 보내지 않게 한다.
        return alarmStreamService.subscribe(currentUserId());
    }

    @PostMapping("/{id}/check")
    public ResponseEntity<?> check(@PathVariable("id") Long id) throws Exception {
        // 없는 알람 번호는 DB 외래키 오류(500)가 아니라 404 로 알린다.
        if (!alarmService.existsAlarm(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "존재하지 않는 알람입니다."));
        }
        String userId = currentUserId();
        alarmService.checkAlarm(id, userId);
        int unreadCount = alarmService.getUnreadCount(userId);
        // 같은 사용자의 다른 탭·PC 헤더 알림 개수도 바로 맞춘다.
        alarmStreamService.publishUnreadCount(userId, unreadCount);
        return ResponseEntity.ok(Map.of("unreadCount", unreadCount));
    }

    /** 요청 본문에서 메모 글자만 꺼낸다. 본문이 없거나 메모가 없으면 null. */
    private static String memoOf(Map<String, Object> body) {
        Object memo = body == null ? null : body.get("memo");
        return memo == null ? null : String.valueOf(memo);
    }

    /** 조치 상태·메모 응답 (조치완료/메모 수정 공통) */
    private static Map<String, Object> actionInfo(AlarmVO alarm) {
        Map<String, Object> info = new HashMap<>();
        info.put("actionYn", alarm.getActionYn());
        info.put("actionUserId", alarm.getActionUserId() == null ? "" : alarm.getActionUserId());
        info.put("actionAt", alarm.getActionAt() == null ? "" : alarm.getActionAt());
        info.put("actionMemo", alarm.getActionMemo() == null ? "" : alarm.getActionMemo());
        info.put("actionMemoAt", alarm.getActionMemoAt() == null ? "" : alarm.getActionMemoAt());
        return info;
    }

    /**
     * 알람 조치완료 (알람당 1건, 누구든 가능). 메모(선택, 최대 200자)를 함께 받을 수 있다.
     * 이미 조치된 알람이면 바꾸지 않고 현재 상태만 돌려준다.
     */
    @PostMapping("/{id}/action")
    public ResponseEntity<?> action(@PathVariable("id") Long id,
                                    @RequestBody(required = false) Map<String, Object> body) throws Exception {
        if (!alarmService.existsAlarm(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "존재하지 않는 알람입니다."));
        }
        String userId = currentUserId();
        int changed;
        try {
            changed = alarmService.actionAlarm(id, userId, memoOf(body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
        AlarmVO alarm = alarmService.getAlarm(id);
        int unreadCount = alarmService.getUnreadCount(userId);
        // 읽음 기록이 함께 생길 수 있으므로 같은 사용자의 다른 탭·PC 헤더 개수도 맞춘다.
        alarmStreamService.publishUnreadCount(userId, unreadCount);
        Map<String, Object> result = actionInfo(alarm);
        result.put("changed", changed > 0);
        result.put("unreadCount", unreadCount);
        return ResponseEntity.ok(result);
    }

    /** 조치 메모 수정 (조치완료한 본인만). 메모를 비우면 메모가 지워진다. 열람은 목록·상세 조회로 누구나 가능하다. */
    @PutMapping("/{id}/memo")
    public ResponseEntity<?> updateMemo(@PathVariable("id") Long id,
                                        @RequestBody(required = false) Map<String, Object> body) throws Exception {
        try {
            alarmService.updateActionMemo(id, currentUserId(), memoOf(body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("code", "ACCESS_DENIED", "message", e.getMessage()));
        }
        return ResponseEntity.ok(actionInfo(alarmService.getAlarm(id)));
    }

    /** 이 사용자의 안 읽은 알람을 모두 확인 처리 */
    @PostMapping("/check-all")
    public ResponseEntity<?> checkAll() throws Exception {
        String userId = currentUserId();
        int checked = alarmService.checkAllAlarms(userId);
        int unreadCount = alarmService.getUnreadCount(userId);
        // 같은 사용자의 다른 탭·PC 헤더 알림 개수도 바로 맞춘다.
        alarmStreamService.publishUnreadCount(userId, unreadCount);
        return ResponseEntity.ok(Map.of("checked", checked, "unreadCount", unreadCount));
    }
}
