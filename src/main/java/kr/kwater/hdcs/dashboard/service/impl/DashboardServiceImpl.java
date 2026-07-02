package kr.kwater.hdcs.dashboard.service.impl;

import lombok.RequiredArgsConstructor;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.stereotype.Service;

import kr.kwater.hdcs.dashboard.dao.DashboardDAO;
import kr.kwater.hdcs.dashboard.service.DashboardService;
import kr.kwater.hdcs.dashboard.vo.DashboardVO;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl extends EgovAbstractServiceImpl implements DashboardService {

    private final DashboardDAO dashboardDAO;

    @Override
    public DashboardVO getSummary() throws Exception {
        DashboardVO vo = new DashboardVO();
        vo.setActiveAlgorithms(dashboardDAO.selectActiveAlgorithmCount());
        vo.setPendingAlarms(dashboardDAO.selectPendingAlarmCount());
        vo.setTotalDevices(dashboardDAO.selectTotalQualityCount());
        vo.setSystemStatus("정상");
        return vo;
    }
}
