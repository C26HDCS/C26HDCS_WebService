package kr.kwater.hdcs.log.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.kwater.hdcs.log.service.LogService;
import kr.kwater.hdcs.log.vo.LogVO;

import java.util.List;

@RestController
@RequestMapping("/api/log")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @GetMapping("/list")
    public ResponseEntity<List<LogVO>> getList(LogVO vo) throws Exception {
        return ResponseEntity.ok(logService.getLogList(vo));
    }
}
