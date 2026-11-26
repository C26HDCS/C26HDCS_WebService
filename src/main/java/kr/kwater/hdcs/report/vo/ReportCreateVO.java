package kr.kwater.hdcs.report.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** 리포트 수동 생성 요청 */
@Getter
@Setter
public class ReportCreateVO {

    /** 조회 기간 (yyyy-MM-dd) */
    private String periodFrom;
    private String periodTo;

    /** 데이터 유형: LOG, EVENT, ALARM, DATA */
    private List<String> dataTypes;

    /**
     * 수신데이터(DATA)의 집계 기준: DAILY(일별), DEVICE(장비별), MONTH(월별), YEAR(연도별), STATION(관측소별).
     * 비어 있으면 기본(대역·상태별 건수 + 상세 목록)으로 만든다. DATA 를 선택하지 않았으면 무시한다.
     */
    private String aggregation;
}
