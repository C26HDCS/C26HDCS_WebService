package kr.kwater.hdcs.dashboard.service.impl;

import java.time.Instant;
import kr.kwater.hdcs.dashboard.service.DashboardService;
import kr.kwater.hdcs.dashboard.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LiveDashboardService {
    private final DashboardService dashboard;
    private final PipelineStatusService pipeline;

    @Transactional
    public LiveDashboardVO snapshot() throws Exception {
        pipeline.refresh();
        var devices = dashboard.getDevices();
        var components = pipeline.components();
        DashboardVO summary = new DashboardVO();
        summary.setTotalDevices(devices.size());
        for (DeviceVO device : devices) {
            device.setPipelineComponents(components.getOrDefault(device.getDeviceId(), java.util.List.of()));
            switch (device.getDeviceStatus() == null ? "" : device.getDeviceStatus()) {
                case "NORMAL" -> summary.setNormal(summary.getNormal() + 1);
                case "WARNING" -> summary.setWarn(summary.getWarn() + 1);
                case "ERROR" -> summary.setFault(summary.getFault() + 1);
                default -> { }
            }
        }
        summary.setSystemStatus(summary.getFault() > 0 ? "장애" : summary.getWarn() > 0 ? "경고" : "정상");
        return new LiveDashboardVO(summary, devices, Instant.now().toString());
    }
}
