package kr.kwater.Hdcs.log;

import java.util.List;
import javax.sql.DataSource;

import kr.kwater.hdcs.log.dao.LogDAO;
import kr.kwater.hdcs.log.service.LogLevel;
import kr.kwater.hdcs.log.service.LogService;
import kr.kwater.hdcs.log.service.impl.LogServiceImpl;
import kr.kwater.hdcs.log.vo.LogVO;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig(LogServiceTest.Config.class)
class LogServiceTest {
    @Autowired private LogService service;
    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactionManager;
    private JdbcTemplate jdbc;

    @BeforeEach
    void prepare() {
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update("DELETE FROM TB_LOG");
        jdbc.update("DELETE FROM TB_EVENT");
        jdbc.update("DELETE FROM TB_DEVICE");
        jdbc.update("DELETE FROM TB_USER");
        jdbc.update("INSERT INTO TB_DEVICE VALUES (75, 'DCS test')");
        jdbc.update("INSERT INTO TB_USER VALUES ('admin')");
    }

    @ParameterizedTest
    @EnumSource(LogLevel.class)
    void savesDeviceLogAndMakesItAvailableToExistingQuery(LogLevel level) throws Exception {
        long id = service.saveLog(75L, level, "DCS received");
        assertThat(id).isPositive();
        LogVO row = service.getLogList(new LogVO()).get(0);
        assertThat(row.getId()).isEqualTo(id);
        assertThat(row.getCategory()).isEqualTo("LOG");
        assertThat(row.getDeviceId()).isEqualTo(75L);
        assertThat(row.getDeviceName()).isEqualTo("DCS test");
        assertThat(row.getLevel()).isEqualTo(level.name());
        assertThat(row.getMessage()).isEqualTo("DCS received");
        assertThat(row.getRegDt()).isNotBlank();
    }

    @Test
    void savesEventAndSupportsExistingFilters() throws Exception {
        long id = service.saveEvent("admin", "SETTING_CHANGED", "Port updated");
        service.saveLog(75L, LogLevel.INFO, "DCS received");
        LogVO filter = new LogVO();
        filter.setSearchCondition("EVENT");
        filter.setSearchLevel("SETTING_CHANGED");
        List<LogVO> rows = service.getLogList(filter);
        assertThat(service.getLogCount(filter)).isEqualTo(1);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getId()).isEqualTo(id);
        assertThat(rows.get(0).getUserId()).isEqualTo("admin");
        assertThat(rows.get(0).getLevel()).isEqualTo("SETTING_CHANGED");
        assertThat(rows.get(0).getMessage()).isEqualTo("Port updated");
        assertThat(rows.get(0).getRegDt()).isNotBlank();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n\t"})
    void rejectsBlankMessages(String message) {
        assertThatThrownBy(() -> service.saveLog(75L, LogLevel.INFO, message))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.saveEvent("admin", "TEST", message))
                .isInstanceOf(IllegalArgumentException.class);
        assertCounts(0, 0);
    }

    @Test
    void validatesIdentifiersLevelsAndColumnLimits() {
        for (Long id : new Long[]{null, 0L, -1L}) {
            assertThatThrownBy(() -> service.saveLog(id, LogLevel.INFO, "test"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> service.saveLog(75L, null, "test"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.saveLog(75L, LogLevel.INFO, "x".repeat(1001)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.saveEvent("admin", "TEST", "x".repeat(1001)))
                .isInstanceOf(IllegalArgumentException.class);
        for (String user : new String[]{null, "", " ", "x".repeat(51)}) {
            assertThatThrownBy(() -> service.saveEvent(user, "TEST", "test"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        for (String type : new String[]{null, "", " ", "x".repeat(31)}) {
            assertThatThrownBy(() -> service.saveEvent("admin", type, "test"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertCounts(0, 0);
    }

    @Test
    void preservesMessagesAtTheColumnBoundaryAndBindsSqlParameters() {
        String prefix = "' ; DELETE FROM TB_USER; --\n";
        String message = prefix + "x".repeat(1000 - prefix.length());
        assertThat(message).hasSize(1000);
        service.saveLog(75L, LogLevel.INFO, message);
        service.saveEvent("admin", "T".repeat(30), message);
        assertThat(jdbc.queryForObject("SELECT MESSAGE FROM TB_LOG", String.class)).isEqualTo(message);
        assertThat(jdbc.queryForObject("SELECT MESSAGE FROM TB_EVENT", String.class)).isEqualTo(message);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TB_USER", Integer.class)).isEqualTo(1);
    }

    @Test
    void propagatesForeignKeyFailuresWithoutCreatingRows() {
        assertThatThrownBy(() -> service.saveLog(999L, LogLevel.ERROR, "test"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> service.saveEvent("missing", "TEST", "test"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertCounts(0, 0);
    }

    @Test
    void rollsBackWithTheCallingBusinessTransaction() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        assertThatThrownBy(() -> transaction.execute(status -> {
            service.saveLog(75L, LogLevel.INFO, "test");
            service.saveEvent("admin", "TEST", "test");
            throw new IllegalStateException("business failed");
        })).isInstanceOf(IllegalStateException.class);
        assertCounts(0, 0);
    }

    private void assertCounts(int logs, int events) {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TB_LOG", Integer.class)).isEqualTo(logs);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM TB_EVENT", Integer.class)).isEqualTo(events);
    }

    @Configuration
    @EnableTransactionManagement
    @ImportResource("classpath:egovframework/spring/context-common.xml")
    static class Config {
        @Bean(destroyMethod = "shutdown")
        org.springframework.jdbc.datasource.embedded.EmbeddedDatabase dataSource() {
            return new EmbeddedDatabaseBuilder().generateUniqueName(true)
                    .setType(EmbeddedDatabaseType.H2)
                    .addScript("sql/log-service-schema.sql").build();
        }

        @Bean
        SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
            SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
            factory.setDataSource(dataSource);
            factory.setMapperLocations(new ClassPathResource("egovframework/mapper/log/log.xml"));
            return factory.getObject();
        }

        @Bean
        MapperFactoryBean<LogDAO> logDAO(SqlSessionFactory factory) {
            MapperFactoryBean<LogDAO> mapper = new MapperFactoryBean<>(LogDAO.class);
            mapper.setSqlSessionFactory(factory);
            return mapper;
        }

        @Bean
        LogService logService(LogDAO dao) {
            return new LogServiceImpl(dao);
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }
}
