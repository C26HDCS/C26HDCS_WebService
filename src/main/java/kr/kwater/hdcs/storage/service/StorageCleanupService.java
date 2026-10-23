package kr.kwater.hdcs.storage.service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Stream;
import kr.kwater.hdcs.log.service.LogRetentionGuard;
import kr.kwater.hdcs.storage.dao.StorageDAO;
import kr.kwater.hdcs.storage.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
public class StorageCleanupService {
    private final StorageCleanupProperties properties;
    private final StorageDAO dao;
    private final StorageDiskSpace disk;
    private final LogRetentionGuard retentionGuard;
    private final StorageAuditService audit;

    @Scheduled(fixedDelayString = "${hdcs.storage.cleanup.interval-ms:60000}",
            initialDelayString = "${hdcs.storage.cleanup.initial-delay-ms:60000}")
    public synchronized void check() {
        if (!properties.isEnabled()) return;
        try {
            runCleanup();
        } catch (Exception ex) {
            log.error("STORAGE_CHECK_FAILED reason={}", ex.getClass().getSimpleName());
            try { audit.record("SYSTEM", "STORAGE_CLEANUP_FAIL", "Storage check failed: " + ex.getClass().getSimpleName()); }
            catch (RuntimeException journalFailure) { log.error("Storage audit persistence unavailable; cleanup stopped"); }
        }
    }

    private void runCleanup() throws IOException {
        if (properties.getDirectory().isBlank()) throw new IllegalArgumentException("Cleanup directory is required");
        Path root = Path.of(properties.getDirectory());
        if (!root.isAbsolute()) throw new IllegalArgumentException("Cleanup directory must be absolute");
        root = root.normalize();
        if (root.getParent() == null || !safeDirectory(root)) throw new IOException("Unsafe cleanup directory");
        Path directory = root;
        StorageSpacePolicyVO spacePolicy = selectSpacePolicy(directory, dao.selectSpacePolicyList());
        if (spacePolicy == null || !Boolean.TRUE.equals(spacePolicy.getEnabled())) return;
        StorageDiskSpace.Space space = disk.read(directory);
        log.info("STORAGE_SPACE_CHECK directory={} usable={} usedPercent={}", directory, space.usable(), space.usedPercent());
        if (!underPressure(space, spacePolicy)) return;

        // Read all policies successfully before deleting anything. No fallback on DB errors.
        int retentionDays = dao.selectRetentionPolicyList().stream()
                .filter(p -> Boolean.TRUE.equals(p.getEnabled()) && "dump".equalsIgnoreCase(p.getFileExtension()))
                .map(StorageRetentionPolicyVO::getRetentionDays).filter(Objects::nonNull)
                .filter(d -> d >= 1).max(Integer::compareTo).orElse(-1);
        if (retentionDays < 1) {
            audit.record("SYSTEM", "STORAGE_SPACE_LOW", "Insufficient space; enabled dump retention policy required: " + directory);
            return;
        }
        List<StorageDeleteTargetVO> rules = dao.selectDeleteTargetList().stream()
                .filter(r -> Boolean.TRUE.equals(r.getEnabled()) && "AUTO".equals(r.getDeleteMode()))
                .filter(r -> "dump".equalsIgnoreCase(r.getFileExtension()))
                .filter(r -> sameDirectory(r.getBasePath(), directory)).toList();
        Instant now = Instant.now();
        List<Path> candidates;
        try (Stream<Path> paths = Files.list(directory)) {
            candidates = paths.filter(p -> eligible(p, rules, retentionDays, now))
                    .sorted(Comparator.comparing(this::modified)).toList();
        }
        int deleted = 0;
        int limit = Math.max(1, Math.min(properties.getMaxFilesPerRun(), 1000));
        for (Path file : candidates) {
            if (deleted >= limit || !underPressure(disk.read(directory), spacePolicy)) break;
            if (!safeDirectory(directory) || !eligible(file, rules, retentionDays, now)) continue;
            BasicFileAttributes before = attributes(file);
            String operation = UUID.randomUUID().toString();
            audit.record("SYSTEM", "STORAGE_DELETE_REQUEST", "AUTO op=" + operation + " path=" + file);
            // Recheck after journal I/O: never delete a marker that has changed since selection.
            if (!safeDirectory(directory) || !eligible(file, rules, retentionDays, now)
                    || !unchanged(before, attributes(file))) {
                audit.record("SYSTEM", "STORAGE_DELETE_SKIPPED", "AUTO op=" + operation + " changed/protected path=" + file);
                continue;
            }
            try {
                Files.delete(file);
            } catch (IOException | SecurityException ex) {
                audit.record("SYSTEM", "STORAGE_DELETE_FAIL", "AUTO op=" + operation + " reason=" + ex.getClass().getSimpleName() + " path=" + file);
                continue;
            }
            deleted++;
            audit.record("SYSTEM", "STORAGE_DELETE_SUCCESS", "AUTO op=" + operation + " bytes=" + before.size() + " path=" + file);
        }
        boolean low = underPressure(disk.read(directory), spacePolicy);
        audit.record("SYSTEM", low ? "STORAGE_SPACE_LOW" : "STORAGE_CLEANUP_DONE",
                "AUTO deleted=" + deleted + " candidates=" + candidates.size() + " spaceLow=" + low + " path=" + directory);
    }

    private boolean eligible(Path file, List<StorageDeleteTargetVO> rules, int days, Instant now) {
        try {
            if (!file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".dump")) return false;
            BasicFileAttributes attrs = attributes(file);
            if (!attrs.isRegularFile() || attrs.isSymbolicLink() || !file.toRealPath().equals(file)
                    || !retentionGuard.canDelete(file)) return false;
            for (StorageDeleteTargetVO rule : rules) {
                int age = Math.max(days, rule.getMinAgeDays() == null ? 0 : rule.getMinAgeDays());
                if (age < 1 || attrs.lastModifiedTime().toInstant().isAfter(now.minus(age, ChronoUnit.DAYS))) continue;
                if (rule.getMinSizeBytes() != null && attrs.size() < rule.getMinSizeBytes()) continue;
                String pattern = rule.getFilePattern();
                if ("FILE".equals(rule.getTargetType())) {
                    if (pattern == null || !file.getFileName().toString().equals(pattern)) continue;
                } else if ("RULE".equals(rule.getTargetType())) {
                    if (pattern == null || pattern.isBlank()) pattern = "*.dump";
                    if (pattern.contains("/") || pattern.contains("\\")
                            || !FileSystems.getDefault().getPathMatcher("glob:" + pattern).matches(file.getFileName())) continue;
                } else continue;
                return true;
            }
        } catch (IOException | RuntimeException ex) { return false; }
        return false;
    }

    private static BasicFileAttributes attributes(Path path) throws IOException {
        return Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
    }

    private static boolean unchanged(BasicFileAttributes a, BasicFileAttributes b) {
        return a.size() == b.size() && a.lastModifiedTime().equals(b.lastModifiedTime())
                && Objects.equals(a.fileKey(), b.fileKey());
    }

    private Instant modified(Path path) {
        try { return attributes(path).lastModifiedTime().toInstant(); }
        catch (IOException ex) { return Instant.MAX; }
    }

    private static boolean safeDirectory(Path directory) throws IOException {
        return Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS) && directory.toRealPath().equals(directory);
    }

    private static boolean sameDirectory(String path, Path directory) {
        try { return path != null && Path.of(path).isAbsolute() && Path.of(path).normalize().equals(directory); }
        catch (InvalidPathException ex) { return false; }
    }

    private static StorageSpacePolicyVO selectSpacePolicy(Path directory, List<StorageSpacePolicyVO> policies) {
        return policies.stream().filter(p -> sameDirectory(p.getTargetPath(), directory)).findFirst()
                .orElseGet(() -> policies.stream().filter(p -> sameDirectory(p.getTargetPath(), directory.getRoot())).findFirst()
                        .orElseGet(() -> policies.stream().filter(p -> "ALL".equalsIgnoreCase(p.getTargetPath())).findFirst().orElse(null)));
    }

    private static boolean underPressure(StorageDiskSpace.Space space, StorageSpacePolicyVO policy) {
        if (space.total() <= 0 || space.usable() < 0 || space.usable() > space.total()) throw new IllegalStateException("Invalid space");
        Double warning = policy.getWarningPercent();
        if (warning == null || !Double.isFinite(warning) || warning < 0 || warning > 100
                || (policy.getMinFreeBytes() != null && policy.getMinFreeBytes() < 0)) throw new IllegalArgumentException("Invalid space policy");
        return space.usedPercent() >= warning || (policy.getMinFreeBytes() != null && space.usable() <= policy.getMinFreeBytes());
    }
}
