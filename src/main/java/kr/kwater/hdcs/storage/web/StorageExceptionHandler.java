package kr.kwater.hdcs.storage.web;

import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = StorageController.class)
public class StorageExceptionHandler {
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> databaseError(DataAccessException error) {
        boolean unavailable = error instanceof DataAccessResourceFailureException;
        return ResponseEntity.status(unavailable ? 503 : 500).body(Map.of(
                "message", unavailable ? "DB 연결에 실패했습니다. 잠시 후 다시 시도해주세요." : "DB 처리 중 오류가 발생했습니다. 서버 로그를 확인해주세요."));
    }
}
