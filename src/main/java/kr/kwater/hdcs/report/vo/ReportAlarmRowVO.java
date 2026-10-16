package kr.kwater.hdcs.report.vo;

import lombok.Getter;
import lombok.Setter;

/** 리포트 상세 목록: 알람 */
@Getter
@Setter
public class ReportAlarmRowVO {

    private String occurredAt;
    private String deviceName;
    private String alarmType;
    private String alarmLevel;
    private String alarmMessage;
}
