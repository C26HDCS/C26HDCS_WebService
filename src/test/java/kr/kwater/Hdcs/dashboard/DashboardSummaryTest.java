package kr.kwater.Hdcs.dashboard;

import kr.kwater.hdcs.dashboard.dao.DashboardDAO;
import kr.kwater.hdcs.dashboard.dao.DataReceiveDAO;
import kr.kwater.hdcs.dashboard.service.impl.DashboardServiceImpl;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

class DashboardSummaryTest {
    DriverManagerDataSource dataSource;
    JdbcTemplate jdbc;

    @BeforeEach void setup() {
        dataSource = new DriverManagerDataSource("jdbc:h2:mem:dashboard_" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE public.tb_device (device_id BIGINT PRIMARY KEY, device_status VARCHAR(20))");
    }

    SqlSession session() throws Exception {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setMapperLocations(new ClassPathResource("egovframework/mapper/dashboard/dashboard.xml"));
        return factory.getObject().openSession();
    }

    @Test void summaryUsesOnlyRealDeviceTablesWithoutLegacyAlgorithmOrAlarmStatus() throws Exception {
        jdbc.update("INSERT INTO tb_device VALUES (1, 'NORMAL'), (2, ' NORMAL '), (3, 'ERROR'), (4, 'WARNING'), (5, NULL)");
        try (SqlSession session = session()) {
            var summary = new DashboardServiceImpl(session.getMapper(DashboardDAO.class), mock(DataReceiveDAO.class)).getSummary();
            assertThat(summary.getTotalDevices()).isEqualTo(5);
            assertThat(summary.getNormal()).isEqualTo(2);
            assertThat(summary.getFault()).isEqualTo(1);
            assertThat(summary.getWarn()).isEqualTo(1);
            var json = new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(summary);
            assertThat(json.has("activeAlgorithms")).isFalse();
            assertThat(json.has("pendingAlarms")).isFalse();
        }
    }

    @Test void emptyDeviceTableReturnsZeroCounts() throws Exception {
        try (SqlSession session = session()) {
            var summary = new DashboardServiceImpl(session.getMapper(DashboardDAO.class), mock(DataReceiveDAO.class)).getSummary();
            assertThat(summary.getTotalDevices()).isZero();
            assertThat(summary.getNormal()).isZero();
            assertThat(summary.getFault()).isZero();
            assertThat(summary.getWarn()).isZero();
        }
    }

    @Test void actualDatabaseFailuresAreNotHiddenAsZeroCounts() throws Exception {
        jdbc.execute("DROP TABLE tb_device");
        try (SqlSession session = session()) {
            var service = new DashboardServiceImpl(session.getMapper(DashboardDAO.class), mock(DataReceiveDAO.class));
            assertThatThrownBy(service::getSummary).isInstanceOf(org.apache.ibatis.exceptions.PersistenceException.class);
        }
    }
}
