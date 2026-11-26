package kr.kwater.hdcs.report.vo;

import lombok.Getter;
import lombok.Setter;

/** 리포트 집계용: 월별/연도별 수신 건수 (수신이 있는 기간만 포함) */
@Getter
@Setter
public class ReportPeriodReceiveVO {

    /** 월별: yyyy-MM, 연도별: yyyy */
    private String period;
    private long dcsCount;
    private long kaCount;
}
