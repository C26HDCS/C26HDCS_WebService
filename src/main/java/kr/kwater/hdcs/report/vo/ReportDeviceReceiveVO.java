package kr.kwater.hdcs.report.vo;

import lombok.Getter;
import lombok.Setter;

/** 리포트 집계용: 장비별 수신 현황 (조회 기간 내, 수신이 없는 장비도 0건으로 포함) */
@Getter
@Setter
public class ReportDeviceReceiveVO {

    private Long deviceId;
    private String deviceName;
    private String deviceType;

    /** 기간 내 수신 건수 (DCS + Ka) */
    private long receiveCount;

    /** 기간 내 마지막 수신 시각 (yyyy-MM-dd HH:mm:ss), 수신이 없으면 null */
    private String lastReceivedAt;
}
