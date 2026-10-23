package kr.kwater.Hdcs.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.kwater.hdcs.common.web.AuthController;
import kr.kwater.hdcs.config.JwtTokenProvider;
import kr.kwater.hdcs.log.config.LogPolicyProperties;
import kr.kwater.hdcs.log.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringJUnitConfig({LogServiceTest.Config.class, AuthAuditTest.Config.class})
class AuthAuditTest {
    @Autowired private AuditEventStore store;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactions;
    @TempDir Path root;
    private AuditEventJournal journal;

    @BeforeEach
    void prepare() {
        jdbc.update("DELETE FROM TB_EVENT");
        jdbc.update("DELETE FROM TB_USER");
        jdbc.update("INSERT INTO TB_USER VALUES ('admin')");
        journal = journal(store);
    }

    private AuditEventJournal journal(AuditEventStore target) {
        LogPolicyProperties properties = new LogPolicyProperties();
        properties.setDirectory(root.toString());
        return new AuditEventJournal(target, new ObjectMapper(), properties);
    }

    @Test
    void authenticationFlowsPersistExistingFilterCodesWithoutPasswords() {
        AuthenticationManager manager = mock(AuthenticationManager.class);
        JwtTokenProvider tokens = mock(JwtTokenProvider.class);
        AuthController controller = new AuthController(manager, tokens, new AuthAuditService(journal));
        var auth = new UsernamePasswordAuthenticationToken("admin", "", java.util.List.of());
        when(manager.authenticate(any())).thenReturn(auth);
        when(tokens.createToken(auth)).thenReturn("test-token");
        assertThat(controller.login(Map.of("username", "admin", "password", "never-log-this")).getStatusCodeValue()).isEqualTo(200);
        when(manager.authenticate(any())).thenThrow(new BadCredentialsException("invalid"));
        assertThat(controller.login(Map.of("username", "unknown", "password", "never-log-this")).getStatusCodeValue()).isEqualTo(401);
        assertThat(controller.logout(auth).getStatusCodeValue()).isEqualTo(204);
        assertThat(controller.logout(null).getStatusCodeValue()).isEqualTo(401);
        assertThat(jdbc.queryForList("SELECT EVENT_TYPE FROM TB_EVENT ORDER BY ID", String.class))
                .containsExactly("LOGIN_SUCCESS", "LOGIN_FAIL", "LOGOUT");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TB_EVENT WHERE MESSAGE LIKE '%never-log-this%'", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT ACTOR_USER_ID FROM TB_EVENT WHERE EVENT_TYPE='LOGIN_FAIL'", String.class)).isEqualTo("unknown");
        assertThat(jdbc.queryForObject("SELECT USER_ID FROM TB_EVENT WHERE EVENT_TYPE='LOGIN_FAIL'", String.class)).isNull();
    }

    @Test
    void userDeletionKeepsTheAuditActor() {
        new AuthAuditService(journal).loginSuccess("admin");
        jdbc.update("DELETE FROM TB_USER WHERE USER_ID='admin'");
        assertThat(jdbc.queryForObject("SELECT COALESCE(USER_ID,ACTOR_USER_ID) FROM TB_EVENT", String.class)).isEqualTo("admin");
    }

    @Test
    void diskSpoolSurvivesDatabaseFailureRestartAndLostCommitAcknowledgement() throws Exception {
        AuditEventStore unavailable = mock(AuditEventStore.class);
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("offline")).when(unavailable).insert(any());
        new AuthAuditService(journal(unavailable)).loginFailure("missing");
        Path pending;
        try (var paths = Files.list(root.resolve("audit-pending"))) { pending = paths.findFirst().orElseThrow(); }
        byte[] durable = Files.readAllBytes(pending);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TB_EVENT", Integer.class)).isZero();
        journal(store).replay();
        assertThat(Files.exists(pending)).isFalse();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TB_EVENT", Integer.class)).isEqualTo(1);
        Files.write(pending, durable);
        journal(store).replay();
        assertThat(Files.exists(pending)).isFalse();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TB_EVENT", Integer.class)).isEqualTo(1);
    }

    @Test
    void authAuditCommitsIndependentlyOfCallingBusinessRollback() {
        assertThatThrownBy(() -> new TransactionTemplate(transactions).execute(status -> {
            new AuthAuditService(journal).loginFailure("admin");
            throw new IllegalStateException("business rollback");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TB_EVENT", Integer.class)).isEqualTo(1);
    }

    @Test
    void corruptedSpoolIsRetainedWithoutBlockingValidEvents() throws Exception {
        Path directory = root.resolve("audit-pending");
        Files.createDirectories(directory);
        Path bad = directory.resolve(UUID.randomUUID() + ".json");
        Files.writeString(bad, "{broken");
        journal.replay();
        assertThat(Files.exists(bad.resolveSibling(bad.getFileName() + ".invalid"))).isTrue();
        new AuthAuditService(journal).logout("admin");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TB_EVENT", Integer.class)).isEqualTo(1);
    }

    @Test
    void refusesLoginIfTheDurableJournalCannotBeWritten() throws Exception {
        Files.writeString(root.resolve("audit-pending"), "not a directory");
        AuthenticationManager manager = mock(AuthenticationManager.class);
        JwtTokenProvider tokens = mock(JwtTokenProvider.class);
        when(manager.authenticate(any())).thenReturn(new UsernamePasswordAuthenticationToken("admin", "", java.util.List.of()));
        AuthController controller = new AuthController(manager, tokens, new AuthAuditService(journal));
        assertThat(controller.login(Map.of("username", "admin", "password", "secret")).getStatusCodeValue()).isEqualTo(503);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TB_EVENT", Integer.class)).isZero();
    }

    @Test
    void replaysCompletedTemporaryEntryLeftByAnInterruptedRename() throws Exception {
        Path directory = root.resolve("audit-pending");
        Files.createDirectories(directory);
        String key = UUID.randomUUID().toString();
        Path temporary = directory.resolve(key + ".tmp");
        AuditEvent event = new AuditEvent(key, "admin", "LOGIN_SUCCESS", "Login succeeded", "2026-10-23T12:00:00");
        new ObjectMapper().writeValue(temporary.toFile(), event);
        Files.setLastModifiedTime(temporary, java.nio.file.attribute.FileTime.from(java.time.Instant.now().minusSeconds(120)));
        journal.replay();
        assertThat(Files.exists(temporary)).isFalse();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TB_EVENT WHERE RECORD_KEY=?", Integer.class, key)).isEqualTo(1);
    }

    @Configuration
    static class Config {
        @Bean JdbcTemplate jdbc(DataSource dataSource) { return new JdbcTemplate(dataSource); }
        @Bean AuditEventStore store(JdbcTemplate jdbc) { return new AuditEventStore(jdbc); }
    }
}
