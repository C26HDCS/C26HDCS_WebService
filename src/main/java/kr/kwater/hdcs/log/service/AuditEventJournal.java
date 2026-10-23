package kr.kwater.hdcs.log.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.kwater.hdcs.log.config.LogPolicyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@Service
public class AuditEventJournal {
    private final AuditEventStore store;
    private final ObjectMapper mapper;
    private final Path pending;
    private final kr.kwater.hdcs.common.DatabaseBackoff retry = new kr.kwater.hdcs.common.DatabaseBackoff();

    public AuditEventJournal(AuditEventStore store, ObjectMapper mapper, LogPolicyProperties properties) {
        this.store = store;
        this.mapper = mapper;
        this.pending = Path.of(properties.getDirectory()).toAbsolutePath().normalize().resolve("audit-pending");
    }

    public synchronized void save(AuditEvent event) {
        validate(event);
        Path temporary = pending.resolve(event.recordKey() + ".tmp");
        Path ready = pending.resolve(event.recordKey() + ".json");
        try {
            Files.createDirectories(pending);
            byte[] bytes = mapper.writeValueAsBytes(event);
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            Files.move(temporary, ready, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            // Callers must not start audited actions without a durable request record.
            throw new UncheckedIOException("Cannot persist audit journal", ex);
        }
        deliver(ready, event);
    }

    public synchronized void replay() {
        if (!Files.isDirectory(pending)) return;
        try (Stream<Path> paths = Files.list(pending)) {
            List<Path> batch = paths.filter(this::isReady)
                    .filter(p -> !Files.isSymbolicLink(p)).limit(200).toList();
            for (Path path : batch) {
                AuditEvent event;
                try {
                    if (Files.size(path) > 16384) throw new IOException("Audit entry exceeds size limit");
                    event = mapper.readValue(path.toFile(), AuditEvent.class);
                    validate(event);
                    String name = path.getFileName().toString();
                    if (!name.equals(event.recordKey() + ".json") && !name.equals(event.recordKey() + ".tmp")) {
                        throw new IllegalArgumentException("Audit key and file name differ");
                    }
                } catch (IOException | IllegalArgumentException ex) {
                    // Preserve damaged records for investigation; do not block valid entries.
                    Files.move(path, path.resolveSibling(path.getFileName() + ".invalid"));
                    log.error("Invalid audit entry retained for investigation: {}", path.getFileName());
                    continue;
                }
                if (!deliver(path, event)) break;
            }
        } catch (IOException ex) {
            log.error("Audit journal replay failed: {}", ex.getClass().getSimpleName());
        }
    }

    private boolean isReady(Path path) {
        String name = path.getFileName().toString();
        if (name.endsWith(".json")) return true;
        try {
            return name.endsWith(".tmp") && Files.getLastModifiedTime(path).toInstant()
                    .isBefore(java.time.Instant.now().minusSeconds(60));
        } catch (IOException ex) {
            return false;
        }
    }

    private boolean deliver(Path path, AuditEvent event) {
        if (!retry.acquire()) return false;
        try {
            try {
                store.insert(event);
            } catch (DuplicateKeyException duplicate) {
                // A lost commit acknowledgement/restart must not create another event.
                if (!store.contains(event.recordKey())) throw duplicate;
            }
            Files.deleteIfExists(path);
            retry.success();
            return true;
        } catch (RuntimeException | IOException ex) {
            log.warn("Audit entry pending retry: key={}, reason={}, retry_ms={}",
                    event.recordKey(), ex.getClass().getSimpleName(), retry.failure());
            return false;
        }
    }

    private static void validate(AuditEvent event) {
        if (event == null || event.recordKey() == null
                || !UUID.fromString(event.recordKey()).toString().equals(event.recordKey())) {
            throw new IllegalArgumentException("Invalid audit record key");
        }
        if (event.actor() == null || event.actor().isBlank() || event.actor().codePointCount(0, event.actor().length()) > 50
                || event.type() == null || !List.of("LOGIN_SUCCESS", "LOGIN_FAIL", "LOGOUT",
                    "STORAGE_DELETE_REQUEST", "STORAGE_DELETE_SUCCESS", "STORAGE_DELETE_FAIL",
                    "STORAGE_DELETE_SKIPPED", "STORAGE_CLEANUP_DONE", "STORAGE_CLEANUP_FAIL",
                    "STORAGE_SPACE_LOW").contains(event.type())
                || event.message() == null || event.message().length() > 1000 || event.occurredAt() == null) {
            throw new IllegalArgumentException("Invalid audit fields");
        }
        try { LocalDateTime.parse(event.occurredAt()); }
        catch (java.time.DateTimeException ex) { throw new IllegalArgumentException("Invalid audit timestamp", ex); }
    }
}
