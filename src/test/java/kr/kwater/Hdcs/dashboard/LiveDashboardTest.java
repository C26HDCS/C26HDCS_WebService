package kr.kwater.Hdcs.dashboard;

import kr.kwater.hdcs.dashboard.service.DashboardService;
import kr.kwater.hdcs.dashboard.service.impl.*;
import kr.kwater.hdcs.dashboard.vo.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class LiveDashboardTest {
    @Test void summaryAndDeviceDetailsAreBuiltFromTheSameDeviceList() throws Exception {
        DashboardService dashboard = mock(DashboardService.class);
        PipelineStatusService pipeline = mock(PipelineStatusService.class);
        List<DeviceVO> devices = new ArrayList<>();
        for (String status : List.of("NORMAL", "WARNING", "ERROR")) {
            DeviceVO device = new DeviceVO();
            device.setDeviceId(String.valueOf(devices.size() + 1));
            device.setDeviceStatus(status);
            devices.add(device);
        }
        var component = new PipelineComponentVO("DCS", "MONITOR", "44/56/01", "WARNING", "stale", "2026-10-23T01:00:00Z", false);
        when(dashboard.getDevices()).thenReturn(devices);
        when(pipeline.components()).thenReturn(Map.of("2", List.of(component)));
        LiveDashboardVO result = new LiveDashboardService(dashboard, pipeline).snapshot();
        assertThat(result.summary().getTotalDevices()).isEqualTo(3);
        assertThat(result.summary().getNormal()).isEqualTo(1);
        assertThat(result.summary().getWarn()).isEqualTo(1);
        assertThat(result.summary().getFault()).isEqualTo(1);
        assertThat(result.devices().get(1).getPipelineComponents()).containsExactly(component);
        assertThat(result.devices().get(0).getPipelineComponents()).isEmpty();
        var order = inOrder(pipeline, dashboard);
        order.verify(pipeline).refresh();
        order.verify(dashboard).getDevices();
        verify(dashboard, never()).getSummary();
    }

    @Test void unavailableDatabaseIsNotReportedAsHealthyOrZeroDevices() {
        DashboardService dashboard = mock(DashboardService.class);
        PipelineStatusService pipeline = mock(PipelineStatusService.class);
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("offline")).when(pipeline).refresh();
        assertThatThrownBy(() -> new LiveDashboardService(dashboard, pipeline).snapshot())
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        verifyNoInteractions(dashboard);
    }
}
