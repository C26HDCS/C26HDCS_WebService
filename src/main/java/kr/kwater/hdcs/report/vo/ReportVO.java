package kr.kwater.hdcs.report.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import kr.kwater.hdcs.common.vo.BaseVO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportVO extends BaseVO {

    private Long reportId;

    /** 생성 사용자 (자동 생성 시 null) */
    private String userId;

    /** 생성자 표시명 (이름 + ID). 상세 조회에서만 채운다. */
    private String userName;

    private String reportName;

    /** 생성 유형: MANUAL(수동) / AUTO(자동) */
    private String generateType;

    /** 조회 기간 (yyyy-MM-dd) */
    private String periodFrom;
    private String periodTo;

    /** 포함 데이터 유형 (쉼표 구분: LOG,EVENT,ALARM,DATA) */
    private String dataTypes;

    private String fileName;

    /** 저장 기준 디렉터리 기준 상대경로. 서버 내부용이므로 API 응답에 노출하지 않는다. */
    @JsonIgnore
    private String filePath;

    /** 서버에 리포트 파일이 실제로 있는지 (목록·상세 조회 시 계산). 다른 PC에서 만든 리포트 등은 false */
    private Boolean fileExists;

    /** 파일 크기(byte) */
    private Long fileSize;

    /** 생성 일시 (yyyy-MM-dd HH:mm:ss) */
    private String regDt;

    /** 검색: 리포트 ID 입력값 (부분 일치, 예: RPT-00, 001, 1) */
    private String searchReportNo;

    private String searchGenerateType;

    /** 검색: 데이터 유형 (LOG/EVENT/ALARM/DATA). 해당 유형을 포함한 리포트를 찾는다. */
    private String searchDataType;
    private String searchDateFrom;
    private String searchDateTo;

    /** 화면 표시용 리포트 ID (예: RPT-001) 1000번 이상은 RPT-1000처럼 자릿수가 늘어난다. */
    public String getReportNo() {
        return reportId == null ? null : String.format("RPT-%03d", reportId);
    }
}
