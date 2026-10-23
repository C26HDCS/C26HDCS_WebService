package kr.kwater.Hdcs.dashboard;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import kr.kwater.hdcs.HdcsWebServiceApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes=HdcsWebServiceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named="HDCS_TEST_APP_PG",matches="1")
class PostgresApplicationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc web;
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        String url=System.getenv("HDCS_TEST_APP_PG_URL");
        URI uri=URI.create(url.substring(5));
        if (!java.util.List.of("localhost","127.0.0.1").contains(uri.getHost()) || !uri.getPath().matches("/hdcs_test_[a-zA-Z0-9_]+"))
            throw new IllegalArgumentException("Disposable local test DB required");
        registry.add("spring.datasource.url",()->url);
        registry.add("spring.datasource.driver-class-name",()->"org.postgresql.Driver");
        registry.add("spring.datasource.username",()->"hdcs_test");
        registry.add("spring.datasource.password",()->System.getenv().getOrDefault("HDCS_TEST_APP_PG_PASSWORD",""));
    }
    @Test void realApplicationProjectsTimeoutRecoveryAndMappingRetirement() throws Exception {
        for (int run=0;run<2;run++) {
            for (String name:java.util.List.of("python_device_status_postgresql.sql","python_status_lifecycle_postgresql.sql")) {
                try(var stream=new ClassPathResource("sql/"+name).getInputStream()) {
                    jdbc.execute(new String(stream.readAllBytes(),StandardCharsets.UTF_8));
                }
            }
        }
        jdbc.update("UPDATE tb_python_component_status SET status='NORMAL',expires_at=CURRENT_TIMESTAMP+INTERVAL '120 seconds'");
        web.perform(get("/api/dashboard/live")).andExpect(status().isOk()).andExpect(jsonPath("$.summary.normal").value(2));
        jdbc.update("UPDATE tb_python_component_status SET expires_at=CURRENT_TIMESTAMP-INTERVAL '1 second' WHERE device_id=75");
        for(int i=0;i<2;i++) web.perform(get("/api/dashboard/live")).andExpect(status().isOk()).andExpect(jsonPath("$.summary.fault").value(1));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tb_alarm",Integer.class)).isEqualTo(1);
        jdbc.update("UPDATE tb_python_component_status SET expires_at=CURRENT_TIMESTAMP+INTERVAL '120 seconds'");
        web.perform(get("/api/dashboard/live")).andExpect(status().isOk()).andExpect(jsonPath("$.summary.normal").value(2));
        jdbc.update("UPDATE tb_python_device_mapping SET device_id=75 WHERE data_kind='KA' AND external_key='cam_test'");
        web.perform(get("/api/dashboard/live")).andExpect(status().isOk()).andExpect(jsonPath("$.summary.fault").value(0));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tb_python_component_status WHERE device_id=86",Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tb_alarm",Integer.class)).isEqualTo(1);
    }
}
