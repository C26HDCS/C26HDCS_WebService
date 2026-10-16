package kr.kwater.hdcs.report.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import kr.kwater.hdcs.report.config.ReportProperties;
import kr.kwater.hdcs.report.service.ReportService;
import kr.kwater.hdcs.report.vo.ReportVO;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/*
 * 리포트 자동 생성 스케줄러.
 * 매주 월요일 00:00에 지난주(월~일) 리포트를 로그, 수신데이터 유형별로 각각 만든다.
 *
 * - report.auto.enabled=true 일 때만 동작한다. (false면 이 클래스는 아예 등록되지 않는다)
 * - 같은 주, 같은 유형의 자동 리포트가 이미 있으면 다시 만들지 않는다.
 * - 그래서 서버가 월요일에 꺼져 있었더라도, 켤 때 지난주 리포트가 없으면 한 번 만들어 준다.
 *   (report.auto.catch-up-on-startup=false 로 끌 수 있다)
 * - 한 유형이 실패해도 다른 유형은 계속 만든다. 실패한 건은 다음 서버 시작 때 다시 시도한다.
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "report.auto", name = "enabled", havingValue = "true")
public class ReportAutoScheduler {

    private final ReportService reportService;
    private final ReportProperties props;

    // 정기 실행과 시작 시 보충이 동시에 돌아 같은 리포트가 두 번 만들어지지 않게, 한 번에 하나만 실행한다.
    private final Object lock = new Object();

    @Scheduled(cron = "${report.auto.cron:0 0 0 * * MON}", zone = "${report.auto.zone:Asia/Seoul}")
    public void runWeekly() {
        generateLastWeek(LocalDate.now(zone()));
    }

    // 서버가 월요일에 꺼져 있었을 때를 대비해, 시작할 때 지난주 리포트가 없으면 만든다.
    @EventListener(ApplicationReadyEvent.class)
    public void catchUpOnStartup() {
        if (!props.getAuto().isCatchUpOnStartup()) {
            return;
        }
        // 서버 시작이 늦어지지 않도록 별도 스레드에서 실행한다.
        CompletableFuture.runAsync(() -> generateLastWeek(LocalDate.now(zone())));
    }

    // 기준일(today)의 지난주 월~일 리포트를 설정된 유형별로 만든다.
  
    void generateLastWeek(LocalDate today) {
        LocalDate thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate from = thisMonday.minusWeeks(1);
        LocalDate to = thisMonday.minusDays(1);

        synchronized (lock) {
            log.info("리포트 자동 생성 시작: {} ~ {}, types={}", from, to, props.getAuto().getDataTypes());
            for (String type : props.getAuto().getDataTypes()) {
                try {
                    ReportVO r = reportService.createAutoReport(from, to, Collections.singletonList(type));
                    if (r != null) {
                        log.info("리포트 자동 생성 완료: {} ({})", r.getReportName(), r.getReportNo());
                    }
                } catch (Exception e) {
                    // 실패해도 다음 유형은 계속 만든다.
                    log.error("리포트 자동 생성 실패: type={}, {} ~ {}", type, from, to, e);
                }
            }
        }
    }

    private ZoneId zone() {
        return ZoneId.of(props.getAuto().getZone());
    }
}
