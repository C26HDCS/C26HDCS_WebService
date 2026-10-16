package kr.kwater.hdcs.report.vo;

import lombok.Getter;
import lombok.Setter;

/** 리포트 상세 목록: 장치 로그 */
@Getter
@Setter
public class ReportLogRowVO {

    private String regDt;
    private String deviceName;
    private String level;
    private String message;
}
