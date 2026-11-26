package kr.kwater.hdcs.report.service.impl;

import kr.kwater.hdcs.report.dao.ReportDataDAO;
import kr.kwater.hdcs.report.vo.ReportCountVO;
import kr.kwater.hdcs.report.vo.ReportData;
import kr.kwater.hdcs.report.vo.ReportSummaryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/** 선택된 데이터 유형별로 리포트 원천 데이터를 조회해 {@link ReportData}에 채운다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportDataCollector {

    private final ReportDataDAO dao;

    public ReportData.LogSection collectLog(String from, String to, int limit) {
        ReportData.LogSection s = new ReportData.LogSection();
        s.setTotal(dao.selectLogTotalCount(from, to));
        s.setLevelCounts(dao.selectLogLevelCounts(from, to));
        s.setDeviceCounts(dao.selectLogDeviceCounts(from, to));
        s.setDetails(dao.selectLogDetails(from, to, limit));
        return s;
    }

    public ReportData.EventSection collectEvent(String from, String to, int limit) {
        ReportData.EventSection s = new ReportData.EventSection();
        s.setTotal(dao.selectEventTotalCount(from, to));
        s.setTypeCounts(dao.selectEventTypeCounts(from, to));
        s.setDetails(dao.selectEventDetails(from, to, limit));
        return s;
    }

    public ReportData.AlarmSection collectAlarm(String from, String to, int limit) {
        ReportData.AlarmSection s = new ReportData.AlarmSection();
        s.setTotal(dao.selectAlarmTotalCount(from, to));
        s.setTypeCounts(dao.selectAlarmTypeCounts(from, to));
        s.setLevelCounts(dao.selectAlarmLevelCounts(from, to));
        s.setDetails(dao.selectAlarmDetails(from, to, limit));
        return s;
    }

    public ReportData.ReceiveSection collectReceive(String from, String to, int limit, String aggregation) {
        ReportData.ReceiveSection s = new ReportData.ReceiveSection();
        List<ReportCountVO> bands = dao.selectReceiveBandCounts(from, to);
        long total = 0;
        for (ReportCountVO b : bands) {
            total += b.getCount();
        }
        s.setTotal((int) Math.min(total, Integer.MAX_VALUE));
        s.setBandCounts(bands);
        // 집계 기준을 골랐으면 선택한 기준의 표만 쓰므로 상태별 건수·상세 목록은 조회하지 않는다. (건수는 개요에서 쓴다)
        if (aggregation == null) {
            s.setDcsStatusCounts(dao.selectDcsStatusCounts(from, to));
            s.setKaStatusCounts(dao.selectKaStatusCounts(from, to));
        }
        // 집계 기준을 골랐을 때만 해당 기준의 표를 채운다. (기본은 일자별·장비별을 조회하지 않는다)
        s.setAggregation(aggregation);
        if ("DAILY".equals(aggregation)) {
            s.setDailyCounts(dao.selectReceiveDailyCounts(from, to));
        }
        if ("DEVICE".equals(aggregation)) {
            s.setDeviceCounts(dao.selectReceiveDeviceCounts(from, to));
        }
        if ("MONTH".equals(aggregation)) {
            s.setMonthlyCounts(dao.selectReceiveMonthlyCounts(from, to));
        }
        if ("YEAR".equals(aggregation)) {
            s.setYearlyCounts(dao.selectReceiveYearlyCounts(from, to));
        }
        if ("STATION".equals(aggregation)) {
            s.setStationCounts(dao.selectReceiveStationCounts(from, to));
        }
        if (aggregation == null) {
            s.setDetails(dao.selectReceiveDetails(from, to, limit));
        }
        return s;
    }

    /** 수신 요약 (건수 기준): 장애율 = 장애/전체, 가동률 = 정상/전체. 소수점 둘째 자리까지 반올림. 전체와 대역별(L/Ka)을 함께 구한다. */
    public ReportData.SummarySection collectSummary(String from, String to) {
        long dcsTotal = 0, dcsOk = 0, kaTotal = 0, kaOk = 0;
        List<ReportSummaryVO> rows = dao.selectReceiveSummary(from, to);
        log.info("수신 요약 집계 조회: {} ~ {}, 결과 {}행", from, to, rows == null ? 0 : rows.size());
        if (rows != null) {
            for (ReportSummaryVO v : rows) {
                log.info("  band={}, total={}, ok={}", v.getBand(), v.getTotal(), v.getOkCount());
                String band = v.getBand() == null ? "" : v.getBand().trim().toUpperCase();
                if ("DCS".equals(band)) {
                    dcsTotal = v.getTotal();
                    dcsOk = v.getOkCount();
                } else if ("KA".equals(band)) {
                    kaTotal = v.getTotal();
                    kaOk = v.getOkCount();
                }
            }
        }
        ReportData.SummarySection all = summarize(dcsTotal + kaTotal, dcsOk + kaOk);
        all.setDcs(summarize(dcsTotal, dcsOk));
        all.setKa(summarize(kaTotal, kaOk));
        return all;
    }

    private static ReportData.SummarySection summarize(long total, long okRaw) {
        long ok = Math.min(okRaw, total);
        ReportData.SummarySection s = new ReportData.SummarySection();
        s.setTotal(total);
        s.setFailCount(total - ok);
        if (total > 0) {
            s.setFailRate(Math.round((total - ok) * 10000.0 / total) / 100.0);
            s.setUptimeRate(Math.round(ok * 10000.0 / total) / 100.0);
        }
        return s;
    }
}
