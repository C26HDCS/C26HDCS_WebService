package kr.kwater.hdcs.alarm.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.kwater.hdcs.alarm.service.AlarmService;
import kr.kwater.hdcs.alarm.vo.AlarmVO;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alarm")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;

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

    @PostMapping("/{id}/check")
    public ResponseEntity<?> check(@PathVariable("id") Long id) throws Exception {
        String userId = currentUserId();
        alarmService.checkAlarm(id, userId);
        int unreadCount = alarmService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("unreadCount", unreadCount));
    }
}
