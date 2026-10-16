package kr.kwater.hdcs.report.vo;

import lombok.Getter;
import lombok.Setter;

/** 리포트 집계용: 일자별 수신 건수 (수신이 없는 날도 0건으로 포함) */
@Getter
@Setter
public class ReportDailyReceiveVO {

    /** yyyy-MM-dd */
    private String day;
    private long dcsCount;
    private long kaCount;
}
