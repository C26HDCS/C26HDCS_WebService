package kr.kwater.hdcs.dashboard.service;

import kr.kwater.hdcs.dashboard.vo.DashboardVO;

public interface DashboardService {

    DashboardVO getSummary() throws Exception;
}
