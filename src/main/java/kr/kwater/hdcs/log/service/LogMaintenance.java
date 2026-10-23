package kr.kwater.hdcs.log.service;

import kr.kwater.hdcs.log.config.LogPolicyProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class LogMaintenance {
    private static final Logger LOG = LoggerFactory.getLogger("hdcs.heartbeat");
    private final AuditEventJournal journal;
    private final AuditEventStore store;
    private final LogPolicyProperties properties;
    private final FileLogSync fileSync;
    private ScheduledExecutorService heartbeat;
    private ScheduledExecutorService maintenance;
    private long lastTick;
    private long ticks;
    @org.springframework.beans.factory.annotation.Value("${hdcs.background.enabled:true}")
    private boolean backgroundEnabled = true;

    @PostConstruct
    public void start() {
        if (!backgroundEnabled) return;
        heartbeat = Executors.newSingleThreadScheduledExecutor(r -> daemon(r, "log-heartbeat"));
        maintenance = Executors.newScheduledThreadPool(2, r -> daemon(r, "log-maintenance"));
        heartbeat.scheduleAtFixedRate(this::tick, 0, 1, TimeUnit.SECONDS);
        maintenance.scheduleWithFixedDelay(() -> safely(journal::replay), 0, 1, TimeUnit.SECONDS);
        maintenance.scheduleWithFixedDelay(() -> safely(this::purgeExpired), 60, 86400, TimeUnit.SECONDS);
    }

    public void tick() {
        long now = System.nanoTime();
        long gap = lastTick == 0 ? 0 : TimeUnit.NANOSECONDS.toMillis(now - lastTick);
        lastTick = now;
        try {
            if (gap > 1500) LOG.error("LOG_INTERVAL_GAP elapsed_ms={}", gap);
            LOG.info("HEARTBEAT component=WebService tick={}", ++ticks);
            fileSync.sync();
            if (ticks % 60 == 0 && Files.getFileStore(Path.of(properties.getDirectory())).getUsableSpace() < 1073741824L) {
                LOG.error("LOG_DISK_LOW free_bytes<1073741824; retention remains five years");
            }
        } catch (Exception ex) {
            // Disk failures must not silently cancel subsequent scheduled attempts.
            System.err.println("LOG_PERSISTENCE_FAILURE " + ex.getClass().getSimpleName());
        }
    }

    public int purgeExpired() {
        if (!properties.isPurgeExpiredEnabled()) return 0;
        int count = store.purgeExpired(LocalDate.now(ZoneId.of("Asia/Seoul")).minusYears(5).atStartOfDay());
        if (count > 0) LOG.info("LOG_RETENTION_PURGE rows={} minimum_years=5", count);
        return count;
    }

    private void safely(Runnable task) {
        try { task.run(); }
        catch (Exception ex) { LOG.error("LOG_MAINTENANCE_FAILURE reason={}", ex.getClass().getSimpleName()); }
    }

    private Thread daemon(Runnable task, String name) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        return thread;
    }

    @PreDestroy
    public void stop() {
        if (heartbeat != null) heartbeat.shutdownNow();
        if (maintenance != null) maintenance.shutdownNow();
        try { fileSync.sync(); }
        catch (Exception ex) { System.err.println("LOG_FINAL_SYNC_FAILURE " + ex.getClass().getSimpleName()); }
    }
}
