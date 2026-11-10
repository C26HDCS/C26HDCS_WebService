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
        factory.setMapperLocations(new ClassPathResource("egovframework/mapper/dashboard/data_receive.xml"),
                new ClassPathResource("egovframework/mapper/dashboard/dashboard.xml"));
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

    @Test void deviceListUsesLinkedStationCoordinatesAndKeepsDevicesWithoutStations() throws Exception {
        jdbc.execute("""
                ALTER TABLE public.tb_device
                ADD (device_name VARCHAR(100), device_type VARCHAR(100), ip_address VARCHAR(100),
                     created_at TIMESTAMP, last_received_at TIMESTAMP,
                     last_receive_status VARCHAR(20), station_id BIGINT, group_id BIGINT)
                """);
        jdbc.execute("""
                CREATE TABLE public.tb_observation_station
                    (station_id BIGINT PRIMARY KEY, name VARCHAR(100), address VARCHAR(500),
                     lat DOUBLE PRECISION, lng DOUBLE PRECISION)
                """);
        jdbc.execute("CREATE TABLE public.tb_device_group (group_id BIGINT PRIMARY KEY, group_name VARCHAR(100))");
        jdbc.execute("""
                CREATE TABLE public.tb_dcs_data
                    (dcs_data_id BIGINT PRIMARY KEY, device_id BIGINT, received_at TIMESTAMP,
                     data_payload VARCHAR(500), received_status VARCHAR(20))
                """);
        jdbc.execute("""
                CREATE TABLE public.tb_dcs_receive_details
                    (id BIGINT PRIMARY KEY, dcs_data_id BIGINT, parse_status VARCHAR(20),
                     quantity_1 VARCHAR(10), quantity_2 VARCHAR(10), quantity_3 VARCHAR(10),
                     quantity_4 VARCHAR(10), voltage VARCHAR(10))
                """);
        jdbc.execute("""
                CREATE TABLE public.tb_ka_data
                    (ka_data_id BIGINT PRIMARY KEY, device_id BIGINT, received_at TIMESTAMP, file_name VARCHAR(255))
                """);
        jdbc.update("INSERT INTO tb_device_group VALUES (5, '영산강')");
        jdbc.update("INSERT INTO tb_observation_station VALUES (12, '관측소', '광주광역시', 35.5, 127.25)");
        jdbc.update("INSERT INTO tb_device (device_id, device_status, station_id, group_id) VALUES (1, ' ERROR ', 12, 5), (2, 'WARNING', NULL, NULL)");
        jdbc.update("""
                INSERT INTO tb_dcs_data VALUES
                (1, 1, TIMESTAMP '2026-11-01 10:00:00', 'old', 'SUCCESS'),
                (2, 1, TIMESTAMP '2026-11-01 10:01:00', 'previous', 'SUCCESS'),
                (3, 1, TIMESTAMP '2026-11-01 10:01:00', 'latest', 'SUCCESS'),
                (4, 1, TIMESTAMP '2026-11-01 10:01:30', 'unparsed', 'SUCCESS')
                """);
        jdbc.update("""
                INSERT INTO tb_dcs_receive_details VALUES
                (1, 2, 'PARSED', '0028', NULL, NULL, NULL, NULL),
                (2, 3, 'PARSED', '0012', '0000', NULL, NULL, '12.50')
                """);
        jdbc.update("INSERT INTO tb_ka_data VALUES (1, 1, TIMESTAMP '2026-11-01 09:59:00', 'older.jpg')");
        try (SqlSession session = session()) {
            var devices = session.getMapper(DashboardDAO.class).selectAllDevices();
            assertThat(devices).hasSize(2);
            assertThat(devices.get(0).getStationName()).isEqualTo("관측소");
            assertThat(devices.get(0).getStationAddress()).isEqualTo("광주광역시");
            assertThat(devices.get(0).getGroupName()).isEqualTo("영산강");
            assertThat(devices.get(0).getLat()).isEqualTo(35.5);
            assertThat(devices.get(0).getLng()).isEqualTo(127.25);
            assertThat(devices.get(0).getDeviceStatus()).isEqualTo("ERROR");
            assertThat(devices.get(1).getLat()).isNull();
            assertThat(devices.get(0).getLatestData()).isEqualTo("측정값 1 0012 / 측정값 2 0000");
            assertThat(devices.get(1).getLatestData()).isNull();
            var history = session.getMapper(DataReceiveDAO.class).selectDcsData("1");
            assertThat(history.get(0).getDataPayload()).isNull();
            assertThat(history.get(1).getDataPayload())
                    .isEqualTo("측정값 1 0012 / 측정값 2 0000 / 전압 12.50");
        }
        jdbc.update("INSERT INTO tb_ka_data VALUES (2, 1, TIMESTAMP '2026-11-01 10:02:00', 'latest.jpg')");
        try (SqlSession session = session()) {
            assertThat(session.getMapper(DashboardDAO.class).selectAllDevices().get(0).getLatestData())
                    .isEqualTo("latest.jpg");
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
