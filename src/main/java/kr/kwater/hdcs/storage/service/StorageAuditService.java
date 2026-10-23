package kr.kwater.hdcs.storage.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import kr.kwater.hdcs.log.service.AuditEvent;
import kr.kwater.hdcs.log.service.AuditEventJournal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StorageAuditService {
    private final AuditEventJournal journal;

    public String manualActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() ? auth.getName() : "<unknown>";
    }

    public void record(String actor, String type, String message) {
        String safeActor = actor.replaceAll("[\\p{Cntrl}]", " ");
        if (safeActor.codePointCount(0, safeActor.length()) > 50) {
            safeActor = safeActor.substring(0, safeActor.offsetByCodePoints(0, 50));
        }
        String safeMessage = message.replaceAll("[\\p{Cntrl}]", " ");
        if (safeMessage.length() > 1000) safeMessage = safeMessage.substring(0, 1000);
        journal.save(new AuditEvent(UUID.randomUUID().toString(), safeActor, type, safeMessage,
                LocalDateTime.now(ZoneId.of("Asia/Seoul")).withNano(0).toString()));
    }
}
