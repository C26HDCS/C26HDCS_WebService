package kr.kwater.hdcs.log.service;

import kr.kwater.hdcs.log.config.LogPolicyProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
public class LogRetentionGuard {
    private final List<Path> roots;

    public LogRetentionGuard(LogPolicyProperties properties) {
        roots = List.of(Path.of(properties.getDirectory()), Path.of(properties.getPythonLogDirectory()));
    }

    public boolean canDelete(Path file) {
        return canDelete(file, Clock.system(ZoneId.of("Asia/Seoul")));
    }

    public boolean canDelete(Path file, Clock clock) {
        try {
            Path real = file.toRealPath();
            for (Path root : roots) {
                if (!Files.exists(root)) continue;
                Path realRoot = root.toRealPath();
                if (real.startsWith(realRoot)) {
                    if (real.startsWith(realRoot.resolve("audit-pending"))) return false;
                    return Files.getLastModifiedTime(real).toInstant().isBefore(
                            LocalDate.now(clock).minusYears(5).atStartOfDay(clock.getZone()).toInstant());
                }
            }
            return true;
        } catch (IOException ex) {
            return false;
        }
    }
}
