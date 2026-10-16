package kr.kwater.hdcs.report.vo;

import lombok.Getter;
import lombok.Setter;

/** 리포트 집계용: 구분값별 건수 (레벨별, 유형별, 장비별, 상태별 등) */
@Getter
@Setter
public class ReportCountVO {

    private String label;
    private long count;
}
