package kr.kwater.hdcs.report.service;

import java.time.LocalDate;
import java.util.List;

import kr.kwater.hdcs.report.vo.ReportCreateVO;
import kr.kwater.hdcs.report.vo.ReportDownload;
import kr.kwater.hdcs.report.vo.ReportVO;

public interface ReportService {

    List<ReportVO> getReportList(ReportVO vo) throws Exception;

    /**
     * 리포트 상세 조회
     *
     * @throws IllegalArgumentException 해당 ID의 리포트가 없는 경우
     */
    ReportVO getReportDetail(Long reportId) throws Exception;

    /**
     * 리포트 파일 다운로드 정보 조회 (저장 경로 검증 포함)
     *
     * @throws IllegalArgumentException 리포트 이력이 없거나 파일이 존재하지 않는 경우
     */
    ReportDownload getReportDownload(Long reportId) throws Exception;

    /**
     * 리포트 PDF 다운로드 정보 조회. 저장된 Word 파일을 PDF로 변환해 두고(최초 1회) 그 파일을 돌려준다.
     *
     * @throws IllegalArgumentException 리포트 이력이 없거나 원본 Word 파일이 존재하지 않는 경우
     * @throws IllegalStateException    PDF 변환에 실패한 경우 (메시지는 화면에 그대로 표시된다)
     */
    ReportDownload getReportPdfDownload(Long reportId) throws Exception;

    /**
     * 리포트 수동 생성: 기간/데이터 유형 검증 → 데이터 조회 → Word 파일 생성·저장 → 이력(TB_REPORT) 등록
     *
     * @throws IllegalArgumentException 요청값이 올바르지 않은 경우 (메시지는 화면에 그대로 표시된다)
     */
    ReportVO createManualReport(ReportCreateVO req, String userId) throws Exception;

    /**
     * 리포트 자동 생성: 지정한 기간·데이터 유형으로 리포트를 만든다. 생성자는 비워 두고(SYSTEM으로 표시) 생성 방식은 AUTO 로 저장한다.
     * 같은 기간·같은 유형으로 이미 자동 생성된 리포트가 있으면 만들지 않고 null 을 돌려준다.
     *
     * @return 생성된 리포트. 이미 있어서 건너뛴 경우 null
     * @throws IllegalArgumentException 기간 또는 데이터 유형이 올바르지 않은 경우
     */
    ReportVO createAutoReport(LocalDate from, LocalDate to, List<String> dataTypes) throws Exception;
}
