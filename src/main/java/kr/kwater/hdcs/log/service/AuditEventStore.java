package kr.kwater.hdcs.log.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditEventStore {
    private final JdbcTemplate jdbc;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insert(AuditEvent event) {
        jdbc.update("INSERT INTO TB_EVENT (RECORD_KEY, USER_ID, ACTOR_USER_ID, EVENT_TYPE, MESSAGE, REG_DT) "
                        + "VALUES (?, (SELECT USER_ID FROM TB_USER WHERE USER_ID = ?), ?, ?, ?, ?)",
                event.recordKey(), event.actor(), event.actor(), event.type(), event.message(),
                Timestamp.valueOf(LocalDateTime.parse(event.occurredAt())));
    }

    public boolean contains(String key) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM TB_EVENT WHERE RECORD_KEY = ?", Integer.class, key);
        return count != null && count > 0;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int purgeExpired(LocalDateTime cutoff) {
        Timestamp before = Timestamp.valueOf(cutoff);
        int logs = jdbc.update("DELETE FROM TB_LOG WHERE ID IN "
                + "(SELECT ID FROM TB_LOG WHERE REG_DT < ? ORDER BY ID LIMIT 1000)", before);
        int events = jdbc.update("DELETE FROM TB_EVENT WHERE ID IN "
                + "(SELECT ID FROM TB_EVENT WHERE REG_DT < ? ORDER BY ID LIMIT 1000)", before);
        return logs + events;
    }
}
