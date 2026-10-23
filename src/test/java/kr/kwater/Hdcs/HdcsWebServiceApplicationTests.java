package kr.kwater.Hdcs;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class HdcsWebServiceApplicationTests {
    @org.springframework.beans.factory.annotation.Autowired
    javax.sql.DataSource dataSource;

    @Test
    void contextLoads() throws Exception {
        try (var connection = dataSource.getConnection()) {
            org.assertj.core.api.Assertions.assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:");
            try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT COUNT(*) FROM TB_DEVICE")) {
                rows.next();
                org.assertj.core.api.Assertions.assertThat(rows.getInt(1)).isEqualTo(2);
            }
        }
    }

}
