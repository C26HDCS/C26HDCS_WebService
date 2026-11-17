package kr.kwater.hdcs.report.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 리포트 본문 생성에 필요한 모든 데이터.
 * 선택하지 않은 데이터 유형의 섹션은 null 이다.
 */
@Getter
@Setter
public class ReportData {

    /** 화면 표시용 리포트 ID (RPT-001) */
    private String reportNo;
    private String reportName;
    private LocalDate periodFrom;
    private LocalDate periodTo;
    private LocalDateTime generatedAt;
    /** 생성자 표시명 (이름 + ID) */
    private String generatedBy;
    private String generateTypeLabel;
    /** 선택된 유형의 화면 표시명 목록 (로그, 이벤트, ...) */
    private List<String> dataTypeLabels;
    /** 상세 목록 최대 행 수 */
    private int maxDetailRows;

    private LogSection log;
    private EventSection event;
    private AlarmSection alarm;
    private ReceiveSection receive;
    /** 수신 요약 리포트일 때만 채운다. 이 경우 다른 섹션은 모두 null 이다. */
    private SummarySection summary;

    @Getter
    @Setter
    public static class LogSection {
        private int total;
        private List<ReportCountVO> levelCounts;
        private List<ReportCountVO> deviceCounts;
        private List<ReportLogRowVO> details;
    }

    @Getter
    @Setter
    public static class EventSection {
        private int total;
        private List<ReportCountVO> typeCounts;
        private List<ReportEventRowVO> details;
    }

    @Getter
    @Setter
    public static class AlarmSection {
        private int total;
        private List<ReportCountVO> typeCounts;
        private List<ReportCountVO> levelCounts;
        private List<ReportAlarmRowVO> details;
    }

    /** 수신데이터 요약: 장애건수, 장애율, 가동률 (건수 기준) */
    @Getter
    @Setter
    public static class SummarySection {
        private long total;
        private long failCount;
        /** 장애율(%) = 장애건수 / 전체 수신건수 × 100. 수신이 없으면 null */
        private Double failRate;
        /** 가동률(%) = 정상 수신건수 / 전체 수신건수 × 100. 수신이 없으면 null */
        private Double uptimeRate;
        /** 대역별 값 (L대역 DCS / Ka대역). 전체 값은 이 객체 자체 */
        private SummarySection dcs;
        private SummarySection ka;
    }

    @Getter
    @Setter
    public static class ReceiveSection {
        private int total;
        private List<ReportCountVO> bandCounts;
        private List<ReportCountVO> dcsStatusCounts;
        private List<ReportCountVO> kaStatusCounts;
        private List<ReportDailyReceiveVO> dailyCounts;
        private List<ReportDeviceReceiveVO> deviceCounts;
        private List<ReportReceiveRowVO> details;
    }
}
