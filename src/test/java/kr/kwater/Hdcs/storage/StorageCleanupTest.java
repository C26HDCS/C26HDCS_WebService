package kr.kwater.Hdcs.storage;

import kr.kwater.hdcs.log.config.LogPolicyProperties;
import kr.kwater.hdcs.log.service.*;
import kr.kwater.hdcs.storage.dao.StorageDAO;
import kr.kwater.hdcs.storage.service.*;
import kr.kwater.hdcs.storage.service.impl.StorageServiceImpl;
import kr.kwater.hdcs.storage.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.dao.DataAccessResourceFailureException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StorageCleanupTest {
    @TempDir Path temp;
    Path root;
    StorageCleanupProperties properties;
    StorageDAO dao;
    StorageDiskSpace disk;
    StorageAuditService audit;
    StorageCleanupService service;
    LogRetentionGuard guard;
    StorageSpacePolicyVO space;
    StorageRetentionPolicyVO retention;
    StorageDeleteTargetVO rule;

    @BeforeEach void setup() throws Exception {
        root = temp.toRealPath();
        properties = new StorageCleanupProperties();
        properties.setEnabled(true);
        properties.setDirectory(root.toString());
        dao = mock(StorageDAO.class);
        disk = mock(StorageDiskSpace.class);
        audit = mock(StorageAuditService.class);
        LogPolicyProperties logs = new LogPolicyProperties();
        logs.setDirectory(root.resolve("logs").toString());
        logs.setPythonLogDirectory(root.resolve("python-logs").toString());
        guard = new LogRetentionGuard(logs);
        space = new StorageSpacePolicyVO();
        space.setTargetPath("ALL");
        space.setEnabled(true);
        space.setWarningPercent(80.0);
        space.setDangerPercent(90.0);
        retention = new StorageRetentionPolicyVO();
        retention.setFileExtension("dump");
        retention.setRetentionDays(7);
        retention.setEnabled(true);
        rule = new StorageDeleteTargetVO();
        rule.setBasePath(root.toString());
        rule.setDeleteMode("AUTO");
        rule.setTargetType("RULE");
        rule.setFilePattern("*.dump");
        rule.setFileExtension("dump");
        rule.setEnabled(true);
        when(dao.selectSpacePolicyList()).thenReturn(List.of(space));
        when(dao.selectRetentionPolicyList()).thenReturn(List.of(retention));
        when(dao.selectDeleteTargetList()).thenReturn(List.of(rule));
        when(disk.read(any())).thenReturn(new StorageDiskSpace.Space(1000, 100));
        service = new StorageCleanupService(properties, dao, disk, guard, audit);
    }

    Path file(String name, int age) throws IOException {
        Path path = root.resolve(name);
        Files.createDirectories(path.getParent());
        Files.writeString(path, "test");
        Files.setLastModifiedTime(path, FileTime.from(Instant.now().minus(age, ChronoUnit.DAYS)));
        return path;
    }

    @Test void requiresExplicitOptIn() throws Exception {
        Path old = file("old.dump", 10);
        properties.setEnabled(false);
        service.check();
        assertThat(old).exists();
        verifyNoInteractions(dao, disk, audit);
        assertThat(new StorageCleanupProperties().isEnabled()).isFalse();
    }

    @Test void onlyExpiredDumpDirectChildrenAreDeleted() throws Exception {
        Path old = file("old.dump", 10);
        Path fresh = file("fresh.dump", 1);
        Path video = file("old.mp4", 10);
        Path log = file("old.log", 10);
        Path nested = file("nested/old.dump", 10);
        Path suffix = file("old.dump.part", 10);
        service.check();
        assertThat(old).doesNotExist();
        for (Path kept : List.of(fresh, video, log, nested, suffix)) assertThat(kept).exists();
        var order = inOrder(audit);
        order.verify(audit).record(eq("SYSTEM"), eq("STORAGE_DELETE_REQUEST"), contains(old.toString()));
        order.verify(audit).record(eq("SYSTEM"), eq("STORAGE_DELETE_SUCCESS"), contains(old.toString()));
        verify(audit).record(eq("SYSTEM"), eq("STORAGE_SPACE_LOW"), contains("deleted=1"));
    }

    @Test void oldestFirstAndStopsWhenSpaceRecovered() throws Exception {
        Path newest = file("newest.dump", 9);
        Path oldest = file("oldest.dump", 12);
        when(disk.read(any())).thenAnswer(call -> new StorageDiskSpace.Space(1000, Files.exists(oldest) ? 100 : 400));
        service.check();
        assertThat(oldest).doesNotExist();
        assertThat(newest).exists();
        verify(audit).record(eq("SYSTEM"), eq("STORAGE_CLEANUP_DONE"), contains("deleted=1"));
    }

    @Test void noDeletionOnHealthyDisk() throws Exception {
        Path old = file("old.dump", 10);
        when(disk.read(any())).thenReturn(new StorageDiskSpace.Space(1000, 500));
        service.check();
        assertThat(old).exists();
        verifyNoInteractions(audit);
    }

    @Test void minimumFreeSpaceAlsoTriggersCleanup() throws Exception {
        Path old = file("old.dump", 10);
        when(disk.read(any())).thenReturn(new StorageDiskSpace.Space(1000, 500));
        space.setMinFreeBytes(600L);
        service.check();
        assertThat(old).doesNotExist();
    }

    @Test void disabledOrMissingPoliciesNeverDelete() throws Exception {
        Path old = file("old.dump", 10);
        retention.setEnabled(false);
        service.check();
        assertThat(old).exists();
        retention.setEnabled(true);
        rule.setEnabled(false);
        service.check();
        assertThat(old).exists();
        rule.setEnabled(true);
        space.setEnabled(false);
        service.check();
        assertThat(old).exists();
    }

    @Test void existingLogRulesAndDifferentFoldersCannotEnableDeletion() throws Exception {
        Path old = file("old.dump", 10);
        rule.setFileExtension("log");
        service.check();
        assertThat(old).exists();
        rule.setFileExtension("dump");
        rule.setBasePath(root.resolve("nested").toString());
        service.check();
        assertThat(old).exists();
    }

    @Test void takesLongerRetentionAndChecksPatternAndSize() throws Exception {
        Path old = file("old.dump", 10);
        rule.setMinAgeDays(11);
        service.check();
        assertThat(old).exists();
        rule.setMinAgeDays(0);
        rule.setMinSizeBytes(100L);
        service.check();
        assertThat(old).exists();
        rule.setMinSizeBytes(null);
        rule.setFilePattern("different*.dump");
        service.check();
        assertThat(old).exists();
        rule.setTargetType("FILE");
        rule.setFilePattern("old.dump");
        service.check();
        assertThat(old).doesNotExist();
    }

    @Test void perRunLimitIsRespected() throws Exception {
        file("first.dump", 12);
        Path second = file("second.dump", 10);
        properties.setMaxFilesPerRun(1);
        service.check();
        assertThat(second).exists();
        verify(audit, times(1)).record(anyString(), eq("STORAGE_DELETE_SUCCESS"), anyString());
    }

    @Test void policyReadFailureNeverDeletesAndNextCheckCanRecover() throws Exception {
        Path old = file("old.dump", 10);
        when(dao.selectDeleteTargetList()).thenThrow(new DataAccessResourceFailureException("offline"));
        service.check();
        assertThat(old).exists();
        verify(audit).record(eq("SYSTEM"), eq("STORAGE_CLEANUP_FAIL"), anyString());
        doReturn(List.of(rule)).when(dao).selectDeleteTargetList();
        service.check();
        assertThat(old).doesNotExist();
    }

    @Test void journalFailureStopsDeletion() throws Exception {
        Path old = file("old.dump", 10);
        doThrow(new UncheckedIOException(new IOException("disk full"))).when(audit)
                .record(anyString(), eq("STORAGE_DELETE_REQUEST"), anyString());
        service.check();
        assertThat(old).exists();
    }

    @Test void markerChangedDuringAuditIsRetained() throws Exception {
        Path old = file("old.dump", 10);
        doAnswer(call -> { Files.writeString(old, "new content"); return null; }).when(audit)
                .record(anyString(), eq("STORAGE_DELETE_REQUEST"), anyString());
        service.check();
        assertThat(old).exists();
        verify(audit).record(eq("SYSTEM"), eq("STORAGE_DELETE_SKIPPED"), anyString());
    }

    @Test void fiveYearLogProtectionAlsoAppliesToDump() throws Exception {
        Path protectedFile = file("logs/old.dump", 10);
        properties.setDirectory(protectedFile.getParent().toString());
        rule.setBasePath(protectedFile.getParent().toString());
        service.check();
        assertThat(protectedFile).exists();
    }

    @Test void blankRelativeAndVolumeRootDirectoriesAreRejected() throws Exception {
        Path old = file("old.dump", 10);
        for (String unsafe : List.of("", ".", root.getRoot().toString())) {
            properties.setDirectory(unsafe);
            service.check();
        }
        assertThat(old).exists();
        verifyNoInteractions(disk);
    }

    @Test void manualDeletionRecordsSuccessAndProtectedSkips() throws Exception {
        Path old = file("old.dump", 10);
        Path protectedFile = file("logs/current.log", 10);
        when(audit.manualActor()).thenReturn("operator");
        StorageServiceImpl manual = new StorageServiceImpl(dao, guard, audit);
        assertThat(manual.deleteFiles(root.toString(), List.of(old.toString())).getDeletedCount()).isEqualTo(1);
        assertThat(manual.deleteFiles(protectedFile.getParent().toString(), List.of(protectedFile.toString())).getSkippedCount()).isEqualTo(1);
        verify(audit).record(eq("operator"), eq("STORAGE_DELETE_SUCCESS"), contains(old.toString()));
        verify(audit).record(eq("operator"), eq("STORAGE_DELETE_SKIPPED"), contains(protectedFile.toString()));
    }

    @Test void storageEventsUseDurableJournalAndRetry() throws Exception {
        LogPolicyProperties logs = new LogPolicyProperties();
        logs.setDirectory(root.resolve("audit-logs").toString());
        AuditEventStore store = mock(AuditEventStore.class);
        doThrow(new DataAccessResourceFailureException("offline")).when(store).insert(any());
        AuditEventJournal journal = new AuditEventJournal(store, new com.fasterxml.jackson.databind.ObjectMapper(), logs);
        StorageAuditService writer = new StorageAuditService(journal);
        writer.record("SYSTEM", "STORAGE_DELETE_SUCCESS", "AUTO path=old.dump");
        Path pending = Path.of(logs.getDirectory()).resolve("audit-pending");
        try (var entries = Files.list(pending)) { assertThat(entries.count()).isEqualTo(1); }
        doNothing().when(store).insert(any());
        new AuditEventJournal(store, new com.fasterxml.jackson.databind.ObjectMapper(), logs).replay();
        try (var entries = Files.list(pending)) { assertThat(entries.count()).isZero(); }
    }

    @Test void disabledSpecificPolicyDoesNotFallBackToEnabledCommonPolicy() throws Exception {
        Path old = file("old.dump", 10);
        StorageSpacePolicyVO specific = new StorageSpacePolicyVO();
        specific.setTargetPath(root.toString());
        specific.setEnabled(false);
        when(dao.selectSpacePolicyList()).thenReturn(List.of(space, specific));
        service.check();
        assertThat(old).exists();
        verifyNoInteractions(disk);
    }

    @Test void manualRulesAndInvalidSpaceNeverDeleteAutomatically() throws Exception {
        Path old = file("old.dump", 10);
        rule.setDeleteMode("MANUAL");
        service.check();
        assertThat(old).exists();
        rule.setDeleteMode("AUTO");
        when(disk.read(any())).thenReturn(new StorageDiskSpace.Space(0, 0));
        service.check();
        assertThat(old).exists();
        verify(audit).record(eq("SYSTEM"), eq("STORAGE_CLEANUP_FAIL"), anyString());
    }
}
