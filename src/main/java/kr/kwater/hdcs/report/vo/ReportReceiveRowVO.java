package kr.kwater.hdcs.report.vo;

import lombok.Getter;
import lombok.Setter;

/** 리포트 상세 목록: 수신데이터 (L대역 DCS + Ka대역) */
@Getter
@Setter
public class ReportReceiveRowVO {

    private String receivedAt;
    private String deviceName;

    /** 수신 대역: DCS(L대역) / KA(Ka대역) */
    private String band;

    /** DCS: 수신 데이터 앞부분(최대 200자), KA: 파일명 */
    private String payload;

    private String status;
}
