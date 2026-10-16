package kr.kwater.hdcs.report.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** application.yml 의 report.* 설정 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "report")
public class ReportProperties {

    /** 리포트 파일 저장 기준 디렉터리 */
    private String storagePath = "./upload/reports";

    /** 조회 기간 최대 일수 (시작일, 종료일 포함) */
    private int maxPeriodDays = 31;

    /** 유형별 상세 목록 최대 행 수 */
    private int maxDetailRows = 1000;

    /** PDF 변환에 쓰는 LibreOffice 실행 파일(soffice) 경로 */
    private String sofficePath = "C:/Program Files/LibreOffice/program/soffice.exe";

    /** PDF 변환 제한 시간(초). 초과하면 변환 프로세스를 강제 종료한다. */
    private int pdfTimeoutSeconds = 120;

    /** 리포트 자동 생성 설정 (report.auto.*) */
    private Auto auto = new Auto();

    @Getter
    @Setter
    public static class Auto {
        /** 자동 생성 사용 여부. 꺼져 있으면 스케줄러 자체가 만들어지지 않는다. */
        private boolean enabled = false;

        /** 서버 시작 때 지난주 리포트가 없으면 바로 만들지 여부. false면 정해진 cron 시각에만 만든다. */
        private boolean catchUpOnStartup = false;

        /** 실행 시각 (Spring cron: 초 분 시 일 월 요일). 기본값은 매주 월요일 00:00 */
        private String cron = "0 0 0 * * MON";

        /** cron 과 '지난주' 계산에 쓰는 시간대 */
        private String zone = "Asia/Seoul";

        /** 자동 생성할 데이터 유형. 유형마다 리포트를 따로 만든다. (LOG, EVENT, ALARM, DATA) */
        private List<String> dataTypes = new ArrayList<>(Arrays.asList("LOG", "DATA"));
    }
}
