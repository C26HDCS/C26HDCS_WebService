package kr.kwater.Hdcs.log;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import kr.kwater.hdcs.log.config.LogPolicyProperties;
import kr.kwater.hdcs.log.service.FileLogSync;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.logback.LogbackLoggingSystem;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class LogbackPolicyTest {
    @TempDir Path directory;

    @Test
    void productionProfileStillPersistsHeartbeatWithFiveYearCompressedRetention() throws Exception {
        MockEnvironment environment = new MockEnvironment().withProperty("hdcs.logging.directory", directory.toString());
        environment.setActiveProfiles("prod");
        LogbackLoggingSystem system = new LogbackLoggingSystem(getClass().getClassLoader());
        try {
            system.cleanUp();
            ((LoggerContext) LoggerFactory.getILoggerFactory()).reset();
            system.beforeInitialize();
            system.initialize(new LoggingInitializationContext(environment), "classpath:logback-spring.xml", null);
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            assertThat(context.getLogger("ROOT").getAppender("CONSOLE")).isNull();
            RollingFileAppender<?> appender = (RollingFileAppender<?>) context.getLogger("ROOT").getAppender("FILE");
            assertThat(appender).isNotNull();
            var policy = (SizeAndTimeBasedRollingPolicy<?>) appender.getRollingPolicy();
            assertThat(policy.getMaxHistory()).isGreaterThanOrEqualTo(1828);
            assertThat(policy.getFileNamePattern()).endsWith(".log.gz");
            LoggerFactory.getLogger("hdcs.heartbeat").info("HEARTBEAT production-test");
            LogPolicyProperties properties = new LogPolicyProperties();
            properties.setDirectory(directory.toString());
            new FileLogSync(properties).sync();
            assertThat(Files.readString(directory.resolve("hdcs-web-service.log"))).contains("HEARTBEAT production-test");
        } finally {
            ((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
            system.cleanUp();
        }
    }
}
