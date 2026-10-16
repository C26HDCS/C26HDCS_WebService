package kr.kwater.hdcs.report.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.kwater.hdcs.report.service.ReportService;
import kr.kwater.hdcs.report.vo.ReportCreateVO;
import kr.kwater.hdcs.report.vo.ReportDownload;
import kr.kwater.hdcs.report.vo.ReportVO;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/list")
    public ResponseEntity<List<ReportVO>> getList(ReportVO vo) throws Exception {
        return ResponseEntity.ok(reportService.getReportList(vo));
    }

    /** 리포트 상세 조회 */
    @GetMapping("/{reportId}")
    public ResponseEntity<?> getDetail(@PathVariable Long reportId) {
        try {
            return ResponseEntity.ok(reportService.getReportDetail(reportId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("리포트 상세 조회 실패: id={}", reportId, e);
            return ResponseEntity.status(500).body(Map.of("message", "리포트 상세 조회 중 오류가 발생했습니다."));
        }
    }

    /** 리포트 Word(.docx) 파일 다운로드 */
    @GetMapping("/{reportId}/download/word")
    public ResponseEntity<?> downloadWord(@PathVariable Long reportId) {
        try {
            return fileResponse(reportService.getReportDownload(reportId),
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("리포트 다운로드 실패: id={}", reportId, e);
            return ResponseEntity.status(500).body(Map.of("message", "리포트 다운로드 중 오류가 발생했습니다."));
        }
    }

    /** 리포트 PDF 파일 다운로드 (최초 요청 시 Word 파일을 변환해서 저장한다) */
    @GetMapping("/{reportId}/download/pdf")
    public ResponseEntity<?> downloadPdf(@PathVariable Long reportId) {
        try {
            return fileResponse(reportService.getReportPdfDownload(reportId), MediaType.APPLICATION_PDF_VALUE);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            // 변환 프로그램이 없거나 변환 실패/시간 초과: 서비스가 화면에 보여줄 메시지를 담아 던진다.
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("리포트 PDF 다운로드 실패: id={}", reportId, e);
            return ResponseEntity.status(500).body(Map.of("message", "리포트 PDF 다운로드 중 오류가 발생했습니다."));
        }
    }

    /**
     * 파일을 첨부 다운로드 응답으로 만든다.
     * 스프링 메서드 파라미터는 null 불가(@NonNull)로 선언돼 있는데, 문자열·Charset·Path 값은 null 여부를 알 수 없는 타입이라
     * IDE가 경고를 띄운다. 여기서 쓰는 값은 모두 null이 아니므로 이 경고만 끈다.
     */
    @SuppressWarnings("null")
    private ResponseEntity<FileSystemResource> fileResponse(ReportDownload dl, String contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(dl.getFileName(), StandardCharsets.UTF_8).build());
        headers.setContentLength(dl.getSize());
        return ResponseEntity.ok().headers(headers).body(new FileSystemResource(dl.getPath()));
    }

    /** 리포트 수동 생성 */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody ReportCreateVO req) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            return ResponseEntity.ok(reportService.createManualReport(req, userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("리포트 생성 실패", e);
            return ResponseEntity.status(500).body(Map.of("message", "리포트 생성 중 오류가 발생했습니다."));
        }
    }
}
