package kr.kwater.hdcs.dashboard.vo;

import java.util.List;

public record LiveDashboardVO(DashboardVO summary, List<DeviceVO> devices, String checkedAt) { }
