package kr.kwater.hdcs.dashboard.service.impl;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.stereotype.Service;

import kr.kwater.hdcs.dashboard.dao.DashboardDAO;
import kr.kwater.hdcs.dashboard.service.DashboardService;
import kr.kwater.hdcs.dashboard.vo.DashboardVO;
import kr.kwater.hdcs.dashboard.vo.DeviceVO;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl extends EgovAbstractServiceImpl implements DashboardService {

    private final DashboardDAO dashboardDAO;

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
}
