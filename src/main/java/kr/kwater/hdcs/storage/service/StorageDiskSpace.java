package kr.kwater.hdcs.storage.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

@Component
public class StorageDiskSpace {
    public record Space(long total, long usable) {
        public double usedPercent() { return (total - usable) * 100.0 / total; }
    }

    public Space read(Path directory) throws IOException {
        var store = Files.getFileStore(directory);
        long total = store.getTotalSpace();
        long usable = store.getUsableSpace();
        if (total <= 0 || usable < 0 || usable > total) throw new IOException("Invalid disk space reading");
        return new Space(total, usable);
    }
}
