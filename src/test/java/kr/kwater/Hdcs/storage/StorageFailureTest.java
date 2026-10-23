package kr.kwater.Hdcs.storage;
import kr.kwater.hdcs.storage.dao.StorageDAO;
import kr.kwater.hdcs.storage.service.impl.StorageServiceImpl;
import kr.kwater.hdcs.storage.vo.*;
import kr.kwater.hdcs.storage.web.*;
import kr.kwater.hdcs.log.service.LogRetentionGuard;
import kr.kwater.hdcs.storage.service.StorageAuditService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class StorageFailureTest {
    @Test void queryFailuresPropagateInsteadOfEmptyOrDefaultPolicies() {
        StorageDAO dao = mock(StorageDAO.class);
        StorageServiceImpl service = new StorageServiceImpl(dao,mock(LogRetentionGuard.class),mock(StorageAuditService.class));
        var offline = new DataAccessResourceFailureException("offline");
        when(dao.selectDeleteTargetList()).thenThrow(offline);
        when(dao.selectRetentionPolicyList()).thenThrow(offline);
        when(dao.selectSpacePolicyList()).thenThrow(offline);
        assertThatThrownBy(service::getDeleteTargets).isSameAs(offline);
        assertThatThrownBy(service::getRetentionPolicies).isSameAs(offline);
        assertThatThrownBy(service::getSpacePolicies).isSameAs(offline);
        assertThatThrownBy(service::getDevices).isSameAs(offline);
    }
    @Test void onlyDuplicateKeysAreReportedAsDuplicateAndErrorsDoNotLeakSql() {
        StorageDAO dao = mock(StorageDAO.class);
        StorageServiceImpl service = new StorageServiceImpl(dao,mock(LogRetentionGuard.class),mock(StorageAuditService.class));
        StorageRetentionPolicyVO policy = new StorageRetentionPolicyVO();
        policy.setFileTypeName("Dump"); policy.setFileExtension("dump"); policy.setRetentionDays(7);
        doThrow(new DataAccessResourceFailureException("sensitive connection details")).when(dao).insertRetentionPolicy(policy);
        assertThatThrownBy(() -> service.createRetentionPolicy(policy)).isInstanceOf(DataAccessResourceFailureException.class);
        doThrow(new DuplicateKeyException("duplicate")).when(dao).insertRetentionPolicy(policy);
        assertThatThrownBy(() -> service.createRetentionPolicy(policy)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("이미 등록");
        var response = new StorageExceptionHandler().databaseError(new DataAccessResourceFailureException("sensitive connection details"));
        assertThat(response.getStatusCodeValue()).isEqualTo(503);
        assertThat(response.getBody().toString()).doesNotContain("sensitive");
    }
}
