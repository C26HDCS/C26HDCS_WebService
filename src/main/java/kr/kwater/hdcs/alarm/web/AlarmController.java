package kr.kwater.hdcs.alarm.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import kr.kwater.hdcs.alarm.service.AlarmService;
import kr.kwater.hdcs.alarm.service.AlarmStreamService;
import kr.kwater.hdcs.alarm.vo.AlarmVO;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

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
