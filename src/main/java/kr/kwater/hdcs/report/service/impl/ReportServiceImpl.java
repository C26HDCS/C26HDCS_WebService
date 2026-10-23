package kr.kwater.hdcs.report.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.stereotype.Service;

import kr.kwater.hdcs.log.service.LogService;
import kr.kwater.hdcs.report.config.ReportProperties;
import kr.kwater.hdcs.report.dao.ReportDAO;
import kr.kwater.hdcs.report.service.ReportGenerator;
import kr.kwater.hdcs.report.service.ReportService;
import kr.kwater.hdcs.report.vo.ReportCreateVO;
import kr.kwater.hdcs.report.vo.ReportData;
import kr.kwater.hdcs.report.vo.ReportDownload;
import kr.kwater.hdcs.report.vo.ReportVO;
import kr.kwater.hdcs.user.dao.UserDAO;
import kr.kwater.hdcs.user.vo.UserVO;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl extends EgovAbstractServiceImpl implements ReportService {

    /** 데이터 유형 코드 (화면 표시 순서이자 리포트명/본문 순서) */
    private static final String[] TYPE_CODES = {"LOG", "EVENT", "ALARM", "DATA"};
    private static final String[] TYPE_LABELS = {"로그", "이벤트", "알람", "수신데이터"};

    /** 리포트 생성 이벤트 유형 (TB_EVENT.EVENT_TYPE). 수동 생성은 사용자 ID, 자동 생성은 대상 SYSTEM 으로 같은 유형을 쓴다. */
    static final String EVENT_REPORT_CREATE_SUCCESS = "REPORT_CREATE_SUCCESS";
    static final String EVENT_REPORT_CREATE_FAIL = "REPORT_CREATE_FAIL";

    private static final DateTimeFormatter COMPACT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final ReportDAO reportDAO;
    private final UserDAO userDAO;
    private final ReportDataCollector collector;
    private final ReportGenerator generator;
    private final ReportProperties props;
    private final PdfConverter pdfConverter;
    private final LogService logService;

    @Override
    public List<ReportVO> getReportList(ReportVO vo) throws Exception {
        vo.setSearchReportNo(escapeLike(vo.getSearchReportNo()));
        List<ReportVO> list = reportDAO.selectReportList(vo);
        for (ReportVO item : list) {
            item.setFileExists(hasFile(item.getFilePath()));
        }
        return list;
    }

    @Override
    public ReportVO getReportDetail(Long reportId) throws Exception {
        ReportVO vo = reportDAO.selectReportById(reportId);
        if (vo == null) {
            throw new IllegalArgumentException("리포트를 찾을 수 없습니다.");
        }
        vo.setUserName(displayUser(vo.getUserId()));
        vo.setFileExists(hasFile(vo.getFilePath()));
        return vo;
    }

    /** 저장 기준 디렉터리 안에 리포트 파일이 실제로 있는지 확인한다. (경로가 기준 디렉터리 밖이면 없는 것으로 본다) */
    private boolean hasFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return false;
        }
        try {
            Path base = Paths.get(props.getStoragePath()).toAbsolutePath().normalize();
            Path path = base.resolve(filePath).normalize();
            return path.startsWith(base) && Files.isRegularFile(path);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public ReportDownload getReportDownload(Long reportId) throws Exception {
        ReportVO vo = reportDAO.selectReportFileInfo(reportId);
        if (vo == null) {
            throw new IllegalArgumentException("리포트를 찾을 수 없습니다.");
        }
        if (vo.getFilePath() == null || vo.getFilePath().trim().isEmpty()) {
            throw new IllegalArgumentException("리포트 파일 정보가 없습니다.");
        }
        // 저장 기준 디렉터리 밖을 가리키는 경로는 허용하지 않는다.
        Path base = Paths.get(props.getStoragePath()).toAbsolutePath().normalize();
        Path path = base.resolve(vo.getFilePath()).normalize();
        if (!path.startsWith(base)) {
            log.warn("허용되지 않은 리포트 파일 경로: id={}, path={}", reportId, vo.getFilePath());
            throw new IllegalArgumentException("리포트 파일을 찾을 수 없습니다.");
        }
        if (!Files.isRegularFile(path)) {
            log.warn("리포트 파일이 존재하지 않음: id={}, path={}", reportId, path);
            throw new IllegalArgumentException("리포트 파일을 찾을 수 없습니다. (삭제되었거나 이동되었을 수 있습니다.)");
        }
        String name = (vo.getFileName() == null || vo.getFileName().trim().isEmpty())
                ? path.getFileName().toString() : vo.getFileName();
        return new ReportDownload(name, path, Files.size(path));
    }

    @Override
    public ReportDownload getReportPdfDownload(Long reportId) throws Exception {
        ReportDownload docx = getReportDownload(reportId);

        // PDF는 Word 파일과 같은 폴더에 같은 이름(확장자만 .pdf)으로 저장한다.
        String docxName = docx.getPath().getFileName().toString();
        Path pdf = docx.getPath().resolveSibling(replaceExtension(docxName, "pdf"));
        pdfConverter.convertIfAbsent(docx.getPath(), pdf);

        return new ReportDownload(replaceExtension(docx.getFileName(), "pdf"), pdf, Files.size(pdf));
    }

    /** 파일명의 확장자를 바꾼다. 확장자가 없으면 뒤에 붙인다. */
    private static String replaceExtension(String name, String ext) {
        int dot = name.lastIndexOf('.');
        return (dot > 0 ? name.substring(0, dot) : name) + "." + ext;
    }

    @Override
    public ReportVO createManualReport(ReportCreateVO req, String userId) throws Exception {
        if (req == null) {
            throw new IllegalArgumentException("요청 정보가 없습니다.");
        }
        LocalDate from = parseDate(req.getPeriodFrom(), "시작일");
        LocalDate to = parseDate(req.getPeriodTo(), "종료일");
        validatePeriod(from, to);

        // 사용자 이벤트(TB_EVENT)용 요약. 알 수 없는 유형 코드는 여기서 같은 메시지의 IllegalArgumentException 으로 걸러진다.
        String types = String.join(",", selectedCodes(selectedTypes(req.getDataTypes())));

        ReportVO created;
        try {
            created = generateReport(from, to, req.getDataTypes(), "MANUAL", userId);
        } catch (IllegalArgumentException e) {
            // 입력값 오류는 리포트 생성을 시도한 것이 아니므로 이벤트를 남기지 않는다.
            throw e;
        } catch (Exception e) {
            recordCreateEvent(userId, EVENT_REPORT_CREATE_FAIL,
                    "리포트 수동 생성 실패 [기간 " + from + " ~ " + to + ", 유형 " + types + "]");
            throw e;
        }
        recordCreateEvent(userId, EVENT_REPORT_CREATE_SUCCESS,
                "리포트 수동 생성 [" + created.getReportName() + " (" + created.getReportNo() + "), 기간 "
                        + from + " ~ " + to + ", 유형 " + types + "]");
        return created;
    }

    /**
     * 리포트 생성 이벤트를 TB_EVENT 에 남긴다.
     * 이 메서드는 호출 시점에 진행 중인 트랜잭션이 없으므로 저장 실패를 잡아도 안전하다.
     * 이벤트 저장이 실패해도 이미 만들어진 리포트 결과는 그대로 돌려줘야 하므로 경고만 남긴다.
     */
    private void recordCreateEvent(String userId, String eventType, String message) {
        try {
            logService.saveEvent(userId, eventType, message);
        } catch (RuntimeException e) {
            log.warn("리포트 생성 이벤트 저장 실패: user={}, type={}", userId, eventType, e);
        }
    }

    /**
     * 자동 생성처럼 로그인 사용자가 없는 리포트 생성 이벤트를 시스템 이벤트(대상 SYSTEM)로 남긴다.
     * 자동 생성 경로에도 진행 중인 트랜잭션이 없으므로 저장 실패를 잡아도 안전하며, 실패해도 리포트 결과에는 영향을 주지 않는다.
     */
    private void recordSystemCreateEvent(String eventType, String message) {
        try {
            logService.saveSystemEvent(eventType, message);
        } catch (RuntimeException e) {
            log.warn("리포트 자동 생성 이벤트 저장 실패: type={}", eventType, e);
        }
    }

    @Override
    public ReportVO createAutoReport(LocalDate from, LocalDate to, List<String> dataTypes) throws Exception {
        if (from == null || to == null) {
            throw new IllegalArgumentException("기간이 올바르지 않습니다.");
        }
        validatePeriod(from, to);

        // 같은 주(기간)·같은 유형의 자동 리포트가 이미 있으면 다시 만들지 않는다. (재시작, 누락 보충 시 중복 방지)
        String codes = String.join(",", selectedCodes(selectedTypes(dataTypes)));
        if (reportDAO.countAutoReport(codes, from.toString(), to.toString()) > 0) {
            log.info("자동 리포트가 이미 있어 건너뜀: types={}, {} ~ {}", codes, from, to);
            return null;
        }
        ReportVO created;
        try {
            created = generateReport(from, to, dataTypes, "AUTO", null);
        } catch (IllegalArgumentException e) {
            // 입력값 오류는 리포트 생성을 시도한 것이 아니므로 이벤트를 남기지 않는다.
            throw e;
        } catch (Exception e) {
            // 자동 생성은 사람이 보고 있지 않으므로 실패도 시스템 이벤트로 남긴다. (수동 생성 실패와 같은 유형, 대상은 SYSTEM)
            recordSystemCreateEvent(EVENT_REPORT_CREATE_FAIL,
                    "리포트 자동 생성 실패 [기간 " + from + " ~ " + to + ", 유형 " + codes + "]");
            throw e;
        }
        // 사용자 이벤트와 같은 유형으로 남기되, 메시지의 '자동 생성'과 대상 SYSTEM 으로 수동 생성과 구분한다.
        recordSystemCreateEvent(EVENT_REPORT_CREATE_SUCCESS,
                "리포트 자동 생성 [" + created.getReportName() + " (" + created.getReportNo() + "), 기간 "
                        + from + " ~ " + to + ", 유형 " + codes + "]");
        return created;
    }

    /** 기간 검증. 수동·자동 생성이 같이 쓴다. */
    private void validatePeriod(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
        }
        if (to.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("종료일은 오늘 이후로 선택할 수 없습니다.");
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days > props.getMaxPeriodDays()) {
            throw new IllegalArgumentException("조회 기간은 최대 " + props.getMaxPeriodDays() + "일까지 선택할 수 있습니다.");
        }
    }

    /** 선택된 유형의 코드 목록 (TYPE_CODES 순서) */
    private static List<String> selectedCodes(boolean[] selected) {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < TYPE_CODES.length; i++) {
            if (selected[i]) {
                codes.add(TYPE_CODES[i]);
            }
        }
        return codes;
    }

    /**
     * 리포트 생성 공통 처리 (수동·자동 공용): 데이터 조회 → 파일 생성·저장 → 이력(TB_REPORT) 등록.
     * 기간 검증은 호출하는 쪽에서 끝낸 상태여야 한다.
     *
     * @param generateType MANUAL 또는 AUTO
     * @param userId       생성자 ID. 자동 생성은 null (화면에는 SYSTEM으로 표시)
     */
    private ReportVO generateReport(LocalDate from, LocalDate to, List<String> requestedTypes,
                                    String generateType, String userId) throws Exception {
        // 1) 데이터 유형
        boolean[] selected = selectedTypes(requestedTypes);
        List<String> codes = selectedCodes(selected);
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < TYPE_CODES.length; i++) {
            if (selected[i]) {
                labels.add(TYPE_LABELS[i]);
            }
        }
        if (codes.isEmpty()) {
            throw new IllegalArgumentException("데이터 유형을 1개 이상 선택해 주세요.");
        }

        // 2) 리포트명: 데이터유형_리포트_시작일_종료일 (전부 선택하면 '전체')
        String typePart = codes.size() == TYPE_CODES.length ? "전체" : String.join("_", labels);
        String reportName = typePart + "_리포트_" + from.format(COMPACT) + "_" + to.format(COMPACT);

        // 3) 데이터 조회
        String f = from.toString();
        String t = to.toString();
        int limit = props.getMaxDetailRows();
        LocalDateTime now = LocalDateTime.now();

        // 리포트 ID 를 미리 발급해 파일 본문에 넣는다. (생성이 실패하면 해당 번호는 건너뛴다)
        ReportVO row = new ReportVO();
        row.setReportId(reportDAO.selectNextReportId());

        ReportData data = new ReportData();
        data.setReportNo(row.getReportNo());
        data.setReportName(reportName);
        data.setPeriodFrom(from);
        data.setPeriodTo(to);
        data.setGeneratedAt(now);
        data.setGeneratedBy(displayUser(userId));
        data.setGenerateTypeLabel("AUTO".equals(generateType) ? "자동" : "수동");
        data.setDataTypeLabels(labels);
        data.setMaxDetailRows(limit);
        if (selected[0]) {
            data.setLog(collector.collectLog(f, t, limit));
        }
        if (selected[1]) {
            data.setEvent(collector.collectEvent(f, t, limit));
        }
        if (selected[2]) {
            data.setAlarm(collector.collectAlarm(f, t, limit));
        }
        if (selected[3]) {
            data.setReceive(collector.collectReceive(f, t, limit));
        }

        // 4) 파일 생성 (임시 파일에 쓴 뒤 이동해서 반쯤 쓰인 파일이 남지 않게 한다)
        //    저장 파일명에는 리포트 ID를 쓰지 않는다. (ID는 INSERT 이후에야 알 수 있고, 다운로드명은 리포트명 사용)
        String ext = generator.getExtension();
        Path base = Paths.get(props.getStoragePath()).toAbsolutePath().normalize();
        String relative = String.format("%04d/%02d/report_%s_%s.%s", now.getYear(), now.getMonthValue(),
                now.format(STAMP), UUID.randomUUID().toString().substring(0, 8), ext);
        Path target = base.resolve(relative).normalize();
        if (!target.startsWith(base)) {
            throw new IllegalStateException("잘못된 리포트 저장 경로입니다.");
        }
        Files.createDirectories(target.getParent());
        Path tmp = Files.createTempFile(target.getParent(), "report_", ".tmp");
        long size;
        try {
            try (OutputStream out = Files.newOutputStream(tmp)) {
                generator.generate(data, out);
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            size = Files.size(target);
        } catch (IOException | RuntimeException e) {
            deleteQuietly(tmp);
            deleteQuietly(target);
            throw e;
        }

        // 5) 이력 등록. 실패하면 방금 만든 파일도 지운다.
        row.setUserId(userId);
        row.setReportName(reportName);
        row.setGenerateType(generateType);
        row.setPeriodFrom(f);
        row.setPeriodTo(t);
        row.setDataTypes(String.join(",", codes));
        row.setFileName(reportName + "." + ext);
        row.setFilePath(relative.replace('\\', '/'));
        row.setFileSize(size);
        try {
            reportDAO.insertReport(row);
        } catch (RuntimeException e) {
            deleteQuietly(target);
            throw e;
        }
        log.info("리포트 생성 완료: id={}, type={}, name={}, size={}B, user={}",
                row.getReportId(), generateType, reportName, size, userId == null ? "SYSTEM" : userId);
        return reportDAO.selectReportById(row.getReportId());
    }

    /** LIKE 검색어의 특수문자(\\, %, _)를 일반 문자로 취급하도록 이스케이프한다. 공백만 있으면 null. */
    private static String escapeLike(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        return input.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static LocalDate parseDate(String s, String label) {
        if (s == null || s.trim().isEmpty()) {
            throw new IllegalArgumentException(label + "을(를) 선택해 주세요.");
        }
        try {
            return LocalDate.parse(s.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(label + " 형식이 올바르지 않습니다. (yyyy-MM-dd)");
        }
    }

    /** 요청의 유형 코드를 TYPE_CODES 순서의 선택 여부 배열로 바꾼다. 알 수 없는 코드는 오류. */
    private static boolean[] selectedTypes(List<String> requested) {
        boolean[] selected = new boolean[TYPE_CODES.length];
        if (requested == null) {
            return selected;
        }
        for (String r : requested) {
            boolean known = false;
            for (int i = 0; i < TYPE_CODES.length; i++) {
                if (TYPE_CODES[i].equalsIgnoreCase(r == null ? "" : r.trim())) {
                    selected[i] = true;
                    known = true;
                }
            }
            if (!known) {
                throw new IllegalArgumentException("알 수 없는 데이터 유형입니다: " + r);
            }
        }
        return selected;
    }

    /** 생성자 표시명. 자동 생성(생성자 없음)은 SYSTEM 으로 표시한다. */
    private String displayUser(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return "SYSTEM";
        }
        try {
            UserVO u = userDAO.findByUserId(userId);
            if (u != null && u.getUserName() != null && !u.getUserName().isEmpty()) {
                return u.getUserName() + " (" + userId + ")";
            }
        } catch (RuntimeException e) {
            log.warn("생성자 이름 조회 실패: {}", userId, e);
        }
        return userId;
    }

    private static void deleteQuietly(Path p) {
        try {
            Files.deleteIfExists(p);
        } catch (IOException ignore) {
            // 정리 실패는 무시
        }
    }
}
