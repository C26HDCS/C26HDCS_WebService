package kr.kwater.hdcs.system.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.kwater.hdcs.system.service.SystemService;
import kr.kwater.hdcs.system.vo.SystemVO;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final SystemService systemService;

    @GetMapping("/info")
    public ResponseEntity<SystemVO> getInfo() throws Exception {
        return ResponseEntity.ok(systemService.getSystemInfo());
    }
}
