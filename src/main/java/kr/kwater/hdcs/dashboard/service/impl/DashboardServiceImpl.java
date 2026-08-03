package kr.kwater.hdcs.dashboard.service.impl;

import java.util.Collections;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.stereotype.Service;

import kr.kwater.hdcs.dashboard.dao.DashboardDAO;
import kr.kwater.hdcs.dashboard.dao.DataReceiveDAO;
import kr.kwater.hdcs.dashboard.service.DashboardService;
import kr.kwater.hdcs.dashboard.vo.DashboardVO;
import kr.kwater.hdcs.dashboard.vo.DeviceStatusHistoryVO;
import kr.kwater.hdcs.dashboard.vo.DeviceVO;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl extends EgovAbstractServiceImpl implements DashboardService {

    private final DashboardDAO    dashboardDAO;
    private final DataReceiveDAO  dataReceiveDAO;

    @Override
    public DashboardVO getSummary() throws Exception {
        DashboardVO vo = new DashboardVO();
        vo.setTotalDevices(dashboardDAO.selectTotalDeviceCount());
        vo.setNormal(dashboardDAO.selectNormalCount());
        vo.setFault(dashboardDAO.selectFaultCount());
        vo.setWarn(dashboardDAO.selectWarnCount());
        vo.setActiveAlgorithms(dashboardDAO.selectActiveAlgorithmCount());
        vo.setPendingAlarms(dashboardDAO.selectPendingAlarmCount());
        vo.setSystemStatus("정상");
        return vo;
    }

    @Override
    public List<DeviceVO> getDevices() throws Exception {
        return dashboardDAO.selectAllDevices();
    }

    @Override
    public List<DeviceStatusHistoryVO> getDeviceHistory(String deviceId) throws Exception {
        return dashboardDAO.selectDeviceHistory(deviceId);
    }

    @Override
    public List<DeviceStatusHistoryVO> getDeviceData(String deviceId, String deviceType) throws Exception {
        log.debug("[getDeviceData] deviceId={}, deviceType={}", deviceId, deviceType);
        if (deviceType == null || deviceType.isBlank()) return Collections.emptyList();
        if (deviceType.contains("L") && deviceType.contains("대역")) {
            log.debug("[getDeviceData] -> selectDcsData");
            List<DeviceStatusHistoryVO> result = dataReceiveDAO.selectDcsData(deviceId);
            log.debug("[getDeviceData] selectDcsData result size={}", result.size());
            return result;
        }
        if (deviceType.contains("Ka")) {
            log.debug("[getDeviceData] -> selectKaData");
            List<DeviceStatusHistoryVO> result = dataReceiveDAO.selectKaData(deviceId);
            log.debug("[getDeviceData] selectKaData result size={}", result.size());
            return result;
        }
        return Collections.emptyList();
    }
}
