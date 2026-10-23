package kr.kwater.hdcs.log.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthAuditService {
    private final AuditEventJournal journal;

    public void loginSuccess(String userId) { save(userId, "LOGIN_SUCCESS", "\uB85C\uADF8\uC778 \uC131\uACF5"); }
    public void loginFailure(String attemptedUserId) { save(attemptedUserId, "LOGIN_FAIL", "\uB85C\uADF8\uC778 \uC2E4\uD328"); }
    public void logout(String userId) { save(userId, "LOGOUT", "\uB85C\uADF8\uC544\uC6C3"); }

    private void save(String userId, String type, String message) {
        String actor = userId == null || userId.isBlank() ? "<unknown>" : userId;
        actor = actor.replaceAll("[\\p{Cntrl}]", " ");
        if (actor.codePointCount(0, actor.length()) > 50) actor = actor.substring(0, actor.offsetByCodePoints(0, 50));
        journal.save(new AuditEvent(UUID.randomUUID().toString(), actor, type, message,
                LocalDateTime.now(ZoneId.of("Asia/Seoul")).withNano(0).toString()));
    }
}
