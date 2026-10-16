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
}
