package kr.kwater.hdcs.dashboard.service;

import java.util.List;

import kr.kwater.hdcs.dashboard.vo.DashboardVO;
import kr.kwater.hdcs.dashboard.vo.DeviceVO;

public interface DashboardService {

    DashboardVO getSummary() throws Exception;

    List<DeviceVO> getDevices() throws Exception;
}
