package kr.kwater.hdcs.report.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
        // 일반 사용자는 시스템이 자동 생성한 리포트만 볼 수 있다. 요청 파라미터와 상관없이 서버에서 고정한다.
        if (!isAdmin()) {
            vo.setSearchGenerateType("AUTO");
        }
        return ResponseEntity.ok(reportService.getReportList(vo));
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    /**
     * 일반 사용자가 자동 생성이 아닌(수동 생성) 리포트에 접근하려 하면 403 응답을 돌려준다. 접근 가능하면 null.
     * 목록만 걸러서는 ID 를 직접 넣어 우회할 수 있어 상세·다운로드에도 같은 검사를 한다.
     */
    private ResponseEntity<?> denyIfManual(Long reportId) {
        if (isAdmin()) return null;
        try {
            ReportVO vo = reportService.getReportDetail(reportId);
            if (!"AUTO".equals(vo.getGenerateType())) {
                return ResponseEntity.status(403).body(Map.of("code", "ACCESS_DENIED", "message", "접근 권한이 없습니다."));
            }
            return null;
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("리포트 권한 확인 실패: id={}", reportId, e);
            return ResponseEntity.status(500).body(Map.of("message", "리포트 조회 중 오류가 발생했습니다."));
        }
    }

    /** 리포트 상세 조회 */
    @GetMapping("/{reportId}")
    public ResponseEntity<?> getDetail(@PathVariable Long reportId) {
        ResponseEntity<?> denied = denyIfManual(reportId);
        if (denied != null) return denied;
        try {
            return ResponseEntity.ok(reportService.getReportDetail(reportId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("리포트 상세 조회 실패: id={}", reportId, e);
            return ResponseEntity.status(500).body(Map.of("message", "리포트 상세 조회 중 오류가 발생했습니다."));
        }
    }

    /** 리포트 한글(.hwpx) 파일 다운로드. (한글 형식으로 바꾸기 전에 만든 예전 리포트는 한글 파일로 변환해서 내려간다) */
    @GetMapping("/{reportId}/download/hwpx")
    public ResponseEntity<?> downloadHwpx(@PathVariable Long reportId) {
        ResponseEntity<?> denied = denyIfManual(reportId);
        if (denied != null) return denied;
        try {
            return fileResponse(reportService.getReportHwpxDownload(reportId), "application/vnd.hancom.hwpx");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            // 예전 리포트를 한글 형식으로 변환하지 못한 경우: 화면에 보여줄 메시지를 담아 던진다.
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("리포트 다운로드 실패: id={}", reportId, e);
            return ResponseEntity.status(500).body(Map.of("message", "리포트 다운로드 중 오류가 발생했습니다."));
        }
    }

    /** 리포트 PDF 파일 다운로드 (최초 요청 시 한글 파일을 RHWP로 변환해서 저장한다) */
    @GetMapping("/{reportId}/download/pdf")
    public ResponseEntity<?> downloadPdf(@PathVariable Long reportId) {
        ResponseEntity<?> denied = denyIfManual(reportId);
        if (denied != null) return denied;
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
