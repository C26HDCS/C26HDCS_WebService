package kr.kwater.hdcs.report.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.kwater.hdcs.report.vo.ReportVO;

import java.util.List;

@Mapper
public interface ReportDAO {

    List<ReportVO> selectReportList(ReportVO vo);

    long selectNextReportId();

    int insertReport(ReportVO vo);

    ReportVO selectReportById(@Param("reportId") Long reportId);

    ReportVO selectReportFileInfo(@Param("reportId") Long reportId);

    /** 같은 기간·같은 유형으로 이미 자동 생성된 리포트 수 (중복 생성 방지용) */
    int countAutoReport(@Param("dataTypes") String dataTypes,
                        @Param("periodFrom") String periodFrom,
                        @Param("periodTo") String periodTo);
}
