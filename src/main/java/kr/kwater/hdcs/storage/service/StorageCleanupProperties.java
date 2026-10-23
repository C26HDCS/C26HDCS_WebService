package kr.kwater.hdcs.storage.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties("hdcs.storage.cleanup")
public class StorageCleanupProperties {
    private boolean enabled;
    private String directory = "";
    private int maxFilesPerRun = 100;
}
