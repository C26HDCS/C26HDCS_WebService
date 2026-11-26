package kr.kwater.hdcs.report.vo;

import lombok.Getter;
import lombok.Setter;

/** 리포트 집계용: 관측소별 수신 현황 (관측소가 지정되지 않은 장비의 수신은 '미지정'으로 묶는다) */
@Getter
@Setter
public class ReportStationReceiveVO {

    private Long stationId;
    private String stationName;

    /** 기간 내 수신이 있었던 장비 수 */
    private long deviceCount;
    private long dcsCount;
    private long kaCount;
}
