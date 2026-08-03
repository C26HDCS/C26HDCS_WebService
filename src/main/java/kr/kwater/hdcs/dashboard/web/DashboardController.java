package kr.kwater.hdcs.dashboard.web;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.kwater.hdcs.dashboard.service.DashboardService;
import kr.kwater.hdcs.dashboard.vo.DashboardVO;
import kr.kwater.hdcs.dashboard.vo.DeviceStatusHistoryVO;
import kr.kwater.hdcs.dashboard.vo.DeviceVO;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardVO> getSummary() throws Exception {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/devices")
    public ResponseEntity<List<DeviceVO>> getDevices() throws Exception {
        return ResponseEntity.ok(dashboardService.getDevices());
    }

    @GetMapping("/devices/{deviceId}/history")
    public ResponseEntity<List<DeviceStatusHistoryVO>> getDeviceHistory(
            @PathVariable String deviceId) throws Exception {
        return ResponseEntity.ok(dashboardService.getDeviceHistory(deviceId));
    }

    @GetMapping("/devices/{deviceId}/data")
    public ResponseEntity<List<DeviceStatusHistoryVO>> getDeviceData(
            @PathVariable String deviceId,
            @RequestParam String deviceType) {
        try {
            List<DeviceStatusHistoryVO> result = dashboardService.getDeviceData(deviceId, deviceType);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[장비 수신 이력 조회 실패] deviceId={}, deviceType={}", deviceId, deviceType, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
