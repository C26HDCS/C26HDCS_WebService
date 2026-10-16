package kr.kwater.hdcs.report.vo;

import lombok.Getter;
import lombok.Setter;

/** 리포트 상세 목록: 사용자 이벤트 */
@Getter
@Setter
public class ReportEventRowVO {

    private String regDt;
    private String userId;
    private String eventType;
    private String message;
}
