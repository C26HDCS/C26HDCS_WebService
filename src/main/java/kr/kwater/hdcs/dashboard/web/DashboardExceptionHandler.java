package kr.kwater.hdcs.dashboard.web;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes=DashboardController.class)
public class DashboardExceptionHandler {
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String,String>> databaseError(DataAccessException error) {
        return ResponseEntity.status(error instanceof org.springframework.dao.DataAccessResourceFailureException ? 503 : 500)
                .body(Map.of("message","상태 정보를 조회하지 못했습니다. DB 연결과 상태 연동 SQL 적용 여부를 확인해주세요."));
    }
}
