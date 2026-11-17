package kr.kwater.hdcs.report.vo;

import lombok.Getter;
import lombok.Setter;

/** 수신데이터 요약 집계: 기간 내 전체 수신 건수와 정상 수신 건수 (DCS + Ka) */
@Getter
@Setter
public class ReportSummaryVO {

    /** 대역: DCS(L대역) / KA(Ka대역) */
    private String band;
    private long total;
    private long okCount;
}
