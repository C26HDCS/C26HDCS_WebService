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
