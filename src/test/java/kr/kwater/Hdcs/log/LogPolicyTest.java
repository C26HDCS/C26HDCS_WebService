package kr.kwater.Hdcs.log;

import kr.kwater.hdcs.log.config.LogPolicyProperties;
import kr.kwater.hdcs.log.service.*;
import kr.kwater.hdcs.storage.dao.StorageDAO;
import kr.kwater.hdcs.storage.service.impl.StorageServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LogPolicyTest {
    @TempDir Path root;

    private LogPolicyProperties properties() {
        LogPolicyProperties properties = new LogPolicyProperties();
        properties.setDirectory(root.resolve("logs").toString());
        properties.setPythonLogDirectory(root.resolve("python-logs").toString());
        return properties;
    }

    @Test
    void heartbeatContinuesEverySecondWithoutBusinessTraffic() throws Exception {
        CountDownLatch ticks = new CountDownLatch(3);
        FileLogSync sync = mock(FileLogSync.class);
        doAnswer(call -> { ticks.countDown(); return null; }).when(sync).sync();
        LogMaintenance maintenance = new LogMaintenance(mock(AuditEventJournal.class), mock(AuditEventStore.class), properties(), sync);
        try {
            maintenance.start();
            assertThat(ticks.await(6, TimeUnit.SECONDS)).isTrue();
        } finally { maintenance.stop(); }
        verify(sync, atLeast(3)).sync();
    }

    @Test
    void retentionHandlesCalendarYearsAndProtectsPendingEntriesIndefinitely() throws Exception {
        LogPolicyProperties properties = properties();
        Path logs = Path.of(properties.getDirectory());
        Files.createDirectories(logs.resolve("audit-pending"));
        Path recent = Files.writeString(logs.resolve("recent.log"), "retain");
        Path old = Files.writeString(logs.resolve("old.log.gz"), "expired");
        Path pending = Files.writeString(logs.resolve("audit-pending/event.json"), "pending");
        FileTime before = FileTime.from(Instant.parse("2021-10-21T00:00:00Z"));
        Files.setLastModifiedTime(old, before);
        Files.setLastModifiedTime(pending, before);
        Clock now = Clock.fixed(Instant.parse("2026-10-23T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        LogRetentionGuard guard = new LogRetentionGuard(properties);
        assertThat(guard.canDelete(recent, now)).isFalse();
        assertThat(guard.canDelete(old, now)).isTrue();
        assertThat(guard.canDelete(pending, now)).isFalse();
        Files.setLastModifiedTime(old, FileTime.from(Instant.parse("2021-10-22T15:00:00Z")));
        assertThat(guard.canDelete(old, now)).isFalse();
    }

    @Test
    void storageManualDeletionCannotBypassLogRetention() throws Exception {
        LogPolicyProperties properties = properties();
        Path logs = Path.of(properties.getDirectory());
        Files.createDirectories(logs);
        Path protectedFile = Files.writeString(logs.resolve("current.log"), "protected");
        StorageServiceImpl service = new StorageServiceImpl(mock(StorageDAO.class), new LogRetentionGuard(properties),
                mock(kr.kwater.hdcs.storage.service.StorageAuditService.class));
        var result = service.deleteFiles(logs.toString(), List.of(protectedFile.toString()));
        assertThat(result.getDeletedCount()).isZero();
        assertThat(result.getSkippedCount()).isEqualTo(1);
        assertThat(Files.exists(protectedFile)).isTrue();
    }

    @Test
    void dbExpiryPurgeRequiresExplicitOptIn() {
        AuditEventStore store = mock(AuditEventStore.class);
        LogPolicyProperties properties = properties();
        LogMaintenance maintenance = new LogMaintenance(mock(AuditEventJournal.class), store, properties, mock(FileLogSync.class));
        assertThat(maintenance.purgeExpired()).isZero();
        verifyNoInteractions(store);
        properties.setPurgeExpiredEnabled(true);
        when(store.purgeExpired(any())).thenReturn(2);
        assertThat(maintenance.purgeExpired()).isEqualTo(2);
    }

    @Test
    void fileSyncForcesTheActiveLogFile() throws Exception {
        LogPolicyProperties properties = properties();
        Files.createDirectories(Path.of(properties.getDirectory()));
        Files.writeString(Path.of(properties.getDirectory()).resolve("hdcs-web-service.log"), "log");
        assertThatCode(() -> new FileLogSync(properties).sync()).doesNotThrowAnyException();
    }
}
