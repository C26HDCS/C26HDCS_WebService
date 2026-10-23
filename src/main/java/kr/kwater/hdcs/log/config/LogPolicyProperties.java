package kr.kwater.hdcs.log.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "hdcs.logging")
public class LogPolicyProperties {
    private String directory = "logs";
    private String pythonLogDirectory = "../C26HDCS_Python/logs";
    private boolean purgeExpiredEnabled = false;
}
