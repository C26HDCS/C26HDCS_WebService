package kr.kwater.hdcs.dashboard.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.kwater.hdcs.dashboard.service.DashboardService;
import kr.kwater.hdcs.dashboard.vo.DashboardVO;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardVO> getSummary() throws Exception {
        return ResponseEntity.ok(dashboardService.getSummary());
    }
}
