package kr.kwater.hdcs.report.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.kwater.hdcs.report.vo.ReportAlarmRowVO;
import kr.kwater.hdcs.report.vo.ReportCountVO;
import kr.kwater.hdcs.report.vo.ReportDailyReceiveVO;
import kr.kwater.hdcs.report.vo.ReportDeviceReceiveVO;
import kr.kwater.hdcs.report.vo.ReportEventRowVO;
import kr.kwater.hdcs.report.vo.ReportLogRowVO;
import kr.kwater.hdcs.report.vo.ReportReceiveRowVO;

import java.util.List;

/**
 * 리포트 본문에 들어갈 원천 데이터 조회/집계.
 * periodFrom, periodTo는 yyyy-MM-dd 문자열이며, 종료일 당일을 포함해 조회한다.
 * (시작일 00:00 이상 ~ 종료일 다음날 00:00 미만)
 */
@Mapper
public interface ReportDataDAO {

    // ── 로그 ──────────────────────────────────────────────
    int selectLogTotalCount(@Param("periodFrom") String periodFrom,
                            @Param("periodTo") String periodTo);

    List<ReportCountVO> selectLogLevelCounts(@Param("periodFrom") String periodFrom,
                                             @Param("periodTo") String periodTo);

    List<ReportCountVO> selectLogDeviceCounts(@Param("periodFrom") String periodFrom,
                                              @Param("periodTo") String periodTo);

    List<ReportLogRowVO> selectLogDetails(@Param("periodFrom") String periodFrom,
                                          @Param("periodTo") String periodTo,
                                          @Param("limit") int limit);

    // ── 이벤트 ────────────────────────────────────────────
    int selectEventTotalCount(@Param("periodFrom") String periodFrom,
                              @Param("periodTo") String periodTo);

    List<ReportCountVO> selectEventTypeCounts(@Param("periodFrom") String periodFrom,
                                              @Param("periodTo") String periodTo);

    List<ReportEventRowVO> selectEventDetails(@Param("periodFrom") String periodFrom,
                                              @Param("periodTo") String periodTo,
                                              @Param("limit") int limit);

    // ── 알람 ──────────────────────────────────────────────
    int selectAlarmTotalCount(@Param("periodFrom") String periodFrom,
                              @Param("periodTo") String periodTo);

    List<ReportCountVO> selectAlarmTypeCounts(@Param("periodFrom") String periodFrom,
                                              @Param("periodTo") String periodTo);

    List<ReportCountVO> selectAlarmLevelCounts(@Param("periodFrom") String periodFrom,
                                               @Param("periodTo") String periodTo);

    List<ReportAlarmRowVO> selectAlarmDetails(@Param("periodFrom") String periodFrom,
                                              @Param("periodTo") String periodTo,
                                              @Param("limit") int limit);

    // ── 수신데이터 ────────────────────────────────────────
    /** 대역별 수신 건수 (label: DCS / KA) */
    List<ReportCountVO> selectReceiveBandCounts(@Param("periodFrom") String periodFrom,
                                                @Param("periodTo") String periodTo);

    /** L대역 DCS 수신 상태별 건수 */
    List<ReportCountVO> selectDcsStatusCounts(@Param("periodFrom") String periodFrom,
                                              @Param("periodTo") String periodTo);

    /** Ka대역 수신 상태별 건수 */
    List<ReportCountVO> selectKaStatusCounts(@Param("periodFrom") String periodFrom,
                                             @Param("periodTo") String periodTo);

    /** 일자별 수신 건수 (기간 내 모든 날짜 포함, 수신 없는 날은 0건) */
    List<ReportDailyReceiveVO> selectReceiveDailyCounts(@Param("periodFrom") String periodFrom,
                                                        @Param("periodTo") String periodTo);

    /** 장비별 수신 건수와 기간 내 마지막 수신 시각 (모든 장비 포함, 수신 없으면 0건) */
    List<ReportDeviceReceiveVO> selectReceiveDeviceCounts(@Param("periodFrom") String periodFrom,
                                                          @Param("periodTo") String periodTo);

    List<ReportReceiveRowVO> selectReceiveDetails(@Param("periodFrom") String periodFrom,
                                                  @Param("periodTo") String periodTo,
                                                  @Param("limit") int limit);
}
