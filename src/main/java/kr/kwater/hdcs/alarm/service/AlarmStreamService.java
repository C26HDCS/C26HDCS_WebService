package kr.kwater.hdcs.alarm.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 헤더 알림 개수를 화면으로 실시간 전달(SSE)하는 서비스
 */
public interface AlarmStreamService {

    /** 사용자의 알림 구독 연결을 만든다. 연결 직후 현재 안 읽은 개수를 한 번 보낸다. */
    SseEmitter subscribe(String userId);

    /** 사용자에게 안 읽은 개수를 보낸다. */
    void publishUnreadCount(String userId, int count);
}