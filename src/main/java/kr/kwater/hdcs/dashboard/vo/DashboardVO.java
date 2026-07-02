package kr.kwater.hdcs.dashboard.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardVO {

    private int    totalDevices;
    private int    activeAlgorithms;
    private int    pendingAlarms;
    private String systemStatus;
}
