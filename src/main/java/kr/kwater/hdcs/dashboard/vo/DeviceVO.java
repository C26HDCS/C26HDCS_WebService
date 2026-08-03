package kr.kwater.hdcs.dashboard.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceVO {
    private String deviceId;
    private String deviceName;
    private String deviceType;
    private String ipAddress;
    private String deviceStatus;
    private String createdAt;
    private String latestReceivedAt;
    private String latestReceivedStatus;
}
