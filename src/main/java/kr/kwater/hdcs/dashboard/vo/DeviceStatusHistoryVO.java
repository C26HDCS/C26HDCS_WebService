package kr.kwater.hdcs.dashboard.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceStatusHistoryVO {
    private String receivedAt;
    private String dataPayload;
    private String receivedStatus;
}
