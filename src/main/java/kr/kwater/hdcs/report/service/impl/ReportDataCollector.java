package kr.kwater.hdcs.report.service.impl;

import kr.kwater.hdcs.report.dao.ReportDataDAO;
import kr.kwater.hdcs.report.vo.ReportCountVO;
import kr.kwater.hdcs.report.vo.ReportData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** 선택된 데이터 유형별로 리포트 원천 데이터를 조회해 {@link ReportData}에 채운다. */
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

    public ReportData.ReceiveSection collectReceive(String from, String to, int limit) {
        ReportData.ReceiveSection s = new ReportData.ReceiveSection();
        List<ReportCountVO> bands = dao.selectReceiveBandCounts(from, to);
        long total = 0;
        for (ReportCountVO b : bands) {
            total += b.getCount();
        }
        s.setTotal((int) Math.min(total, Integer.MAX_VALUE));
        s.setBandCounts(bands);
        s.setDcsStatusCounts(dao.selectDcsStatusCounts(from, to));
        s.setKaStatusCounts(dao.selectKaStatusCounts(from, to));
        s.setDailyCounts(dao.selectReceiveDailyCounts(from, to));
        s.setDeviceCounts(dao.selectReceiveDeviceCounts(from, to));
        s.setDetails(dao.selectReceiveDetails(from, to, limit));
        return s;
    }
}
