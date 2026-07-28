package kr.kwater.hdcs.dashboard.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceVO {
    private String deviceId;
    private String deviceName;
    private String ipAddress;
    private String deviceStatus;
}
