package kr.kwater.Hdcs.storage;

import java.net.URI;
import java.util.UUID;
import kr.kwater.hdcs.storage.dao.StorageDAO;
import kr.kwater.hdcs.storage.vo.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "HDCS_TEST_STORAGE_PG", matches = "1")
class StorageMapperPostgresTest {
    @Test void allStorageInsertsReturnTheirPostgresGeneratedId() throws Exception {
        String url = System.getenv("HDCS_TEST_STORAGE_PG_URL");
        URI uri = URI.create(url.substring("jdbc:".length()));
        if (!"postgresql".equals(uri.getScheme()) || !java.util.List.of("127.0.0.1", "localhost").contains(uri.getHost())
                || !uri.getPath().matches("/hdcs_test_[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException("Disposable localhost hdcs_test_* database required");
        }
        SingleConnectionDataSource source = new SingleConnectionDataSource(url,
                System.getenv().getOrDefault("HDCS_TEST_STORAGE_PG_USER", "hdcs_test"),
                System.getenv().getOrDefault("HDCS_TEST_STORAGE_PG_PASSWORD", ""), true);
        try {
            for (String script : java.util.List.of("storage_delete_target.sql", "storage_retention_policy.sql", "storage_space_policy.sql")) {
                ScriptUtils.executeSqlScript(source.getConnection(), new ClassPathResource("sql/" + script));
            }
            try (var wrongCase = source.getConnection().prepareStatement(
                    "INSERT INTO tb_storage_delete_target(target_name,delete_mode,target_type,base_path) "
                            + "VALUES ('case-test','MANUAL','RULE','D:/isolated-storage-test')", new String[]{"ID"})) {
                assertThatThrownBy(wrongCase::executeUpdate).isInstanceOf(java.sql.SQLException.class)
                        .satisfies(ex -> assertThat(((java.sql.SQLException) ex).getSQLState()).isEqualTo("42703"));
            }
            SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
            factory.setDataSource(source);
            factory.setMapperLocations(new ClassPathResource("egovframework/mapper/storage/storage.xml"));
            try (var session = factory.getObject().openSession(false)) {
                StorageDAO dao = session.getMapper(StorageDAO.class);
                String suffix = UUID.randomUUID().toString().replace("-", "");
                StorageDeleteTargetVO target = new StorageDeleteTargetVO();
                target.setTargetName("mapper-test-" + suffix);
                target.setDeleteMode("MANUAL");
                target.setTargetType("RULE");
                target.setBasePath("D:/isolated-storage-test");
                target.setFilePattern("*.dump");
                target.setFileExtension("dump");
                target.setEnabled(false);
                assertThat(dao.insertDeleteTarget(target)).isEqualTo(1);
                assertThat(target.getId()).isPositive();
                assertThat(dao.selectDeleteTargetList()).anyMatch(v -> target.getId().equals(v.getId()));

                StorageRetentionPolicyVO retention = new StorageRetentionPolicyVO();
                retention.setFileTypeName("Mapper test");
                retention.setFileExtension(suffix.substring(0, 20));
                retention.setRetentionDays(7);
                retention.setEnabled(false);
                assertThat(dao.insertRetentionPolicy(retention)).isEqualTo(1);
                assertThat(retention.getId()).isPositive();
                assertThat(dao.selectRetentionPolicyList()).anyMatch(v -> retention.getId().equals(v.getId()));

                StorageSpacePolicyVO space = new StorageSpacePolicyVO();
                space.setTargetPath("D:/isolated-storage-test/" + suffix);
                space.setWarningPercent(80.0);
                space.setDangerPercent(90.0);
                space.setEnabled(false);
                assertThat(dao.insertSpacePolicy(space)).isEqualTo(1);
                assertThat(space.getId()).isPositive();
                assertThat(dao.selectSpacePolicyList()).anyMatch(v -> space.getId().equals(v.getId()));
                session.rollback();
            }
        } finally { source.destroy(); }
    }
}
