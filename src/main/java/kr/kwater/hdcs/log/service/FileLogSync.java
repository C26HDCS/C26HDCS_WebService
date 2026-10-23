package kr.kwater.hdcs.log.service;

import kr.kwater.hdcs.log.config.LogPolicyProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Component
public class FileLogSync {
    private final Path active;

    public FileLogSync(LogPolicyProperties properties) {
        active = Path.of(properties.getDirectory()).toAbsolutePath().normalize().resolve("hdcs-web-service.log");
    }

    public void sync() throws IOException {
        try (FileChannel channel = FileChannel.open(active, StandardOpenOption.WRITE)) {
            channel.force(true);
        }
    }
}
