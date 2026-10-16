package kr.kwater.hdcs.report.service.impl;

import kr.kwater.hdcs.report.service.ReportGenerator;
import kr.kwater.hdcs.report.vo.ReportAlarmRowVO;
import kr.kwater.hdcs.report.vo.ReportCountVO;
import kr.kwater.hdcs.report.vo.ReportData;
import kr.kwater.hdcs.report.vo.ReportDailyReceiveVO;
import kr.kwater.hdcs.report.vo.ReportDeviceReceiveVO;
import kr.kwater.hdcs.report.vo.ReportEventRowVO;
import kr.kwater.hdcs.report.vo.ReportLogRowVO;
import kr.kwater.hdcs.report.vo.ReportReceiveRowVO;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.XWPFTableCell.XWPFVertAlign;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGrid;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblLayoutType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Word(.docx) 리포트 생성기.
 *
 * 홍수통제소 양식(template)이 확정되기 전까지의 임시 본문 구성이다.
 * 양식이 정해지면 이 클래스의 본문 작성부만 교체하면 된다.
 */
@Component
public class WordReportGenerator implements ReportGenerator {

    private static final String FONT = "맑은 고딕";
    /** A4 (twips) */
    private static final int PAGE_W = 11906;
    private static final int PAGE_H = 16838;
    private static final int MARGIN = 1134; // 2cm
    private static final int BODY_W = PAGE_W - MARGIN * 2;

    private static final String HEADER_FILL = "D9E2F3";
    private static final String LABEL_FILL = "F2F2F2";
    private static final String NOTE_COLOR = "666666";
    private static final int MAX_CELL_CHARS = 200;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public String getExtension() {
        return "docx";
    }

    @Override
    public void generate(ReportData data, OutputStream out) throws IOException {
        try (XWPFDocument doc = new XWPFDocument()) {
            setupPage(doc);
            setupFooter(doc);

            writeTitle(doc, data);
            writeInfo(doc, data);

            int no = 1;
            writeOverview(doc, data, no++);
            if (data.getLog() != null) {
                writeLog(doc, data, no++);
            }
            if (data.getEvent() != null) {
                writeEvent(doc, data, no++);
            }
            if (data.getAlarm() != null) {
                writeAlarm(doc, data, no++);
            }
            if (data.getReceive() != null) {
                writeReceive(doc, data, no++);
            }

            doc.write(out);
        }
    }

    // ── 페이지 / 공통 ────────────────────────────────────────────

    private void setupPage(XWPFDocument doc) {
        CTSectPr sect = doc.getDocument().getBody().isSetSectPr()
                ? doc.getDocument().getBody().getSectPr()
                : doc.getDocument().getBody().addNewSectPr();
        CTPageSz sz = sect.isSetPgSz() ? sect.getPgSz() : sect.addNewPgSz();
        sz.setW(BigInteger.valueOf(PAGE_W));
        sz.setH(BigInteger.valueOf(PAGE_H));
        CTPageMar mar = sect.isSetPgMar() ? sect.getPgMar() : sect.addNewPgMar();
        BigInteger m = BigInteger.valueOf(MARGIN);
        mar.setTop(m);
        mar.setBottom(m);
        mar.setLeft(m);
        mar.setRight(m);
        mar.setHeader(BigInteger.valueOf(567));
        mar.setFooter(BigInteger.valueOf(567));
    }

    private void setupFooter(XWPFDocument doc) {
        XWPFFooter footer = doc.createFooter(HeaderFooterType.DEFAULT);
        XWPFParagraph p = footer.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);

        XWPFRun r1 = p.createRun();
        font(r1, 18, false, NOTE_COLOR);
        r1.setText("- ");

        XWPFRun begin = p.createRun();
        font(begin, 18, false, NOTE_COLOR);
        begin.getCTR().addNewFldChar().setFldCharType(STFldCharType.BEGIN);
        XWPFRun instr = p.createRun();
        font(instr, 18, false, NOTE_COLOR);
        instr.getCTR().addNewInstrText().setStringValue(" PAGE ");
        XWPFRun sep = p.createRun();
        font(sep, 18, false, NOTE_COLOR);
        sep.getCTR().addNewFldChar().setFldCharType(STFldCharType.SEPARATE);
        XWPFRun num = p.createRun();
        font(num, 18, false, NOTE_COLOR);
        num.setText("1");
        XWPFRun end = p.createRun();
        font(end, 18, false, NOTE_COLOR);
        end.getCTR().addNewFldChar().setFldCharType(STFldCharType.END);

        XWPFRun r2 = p.createRun();
        font(r2, 18, false, NOTE_COLOR);
        r2.setText(" -");
    }

    /** 한글이 깨지지 않도록 영문/한글 글꼴을 모두 지정한다. sizeHalfPt: 반포인트 단위(20 = 10pt, 짝수만 사용) */
    private void font(XWPFRun run, int sizeHalfPt, boolean bold, String color) {
        run.setBold(bold);
        run.setFontSize(sizeHalfPt / 2);
        if (color != null) {
            run.setColor(color);
        }
        run.setFontFamily(FONT);
        run.setFontFamily(FONT, XWPFRun.FontCharRange.ascii);
        run.setFontFamily(FONT, XWPFRun.FontCharRange.hAnsi);
        run.setFontFamily(FONT, XWPFRun.FontCharRange.eastAsia);
        run.setFontFamily(FONT, XWPFRun.FontCharRange.cs);
    }

    private XWPFParagraph paragraph(XWPFDocument doc, String text, int sizeHalfPt, boolean bold,
                                    ParagraphAlignment align, String color, int spaceBefore, int spaceAfter) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(align);
        p.setSpacingBefore(spaceBefore);
        p.setSpacingAfter(spaceAfter);
        XWPFRun r = p.createRun();
        font(r, sizeHalfPt, bold, color);
        r.setText(clean(text));
        return p;
    }

    private void heading(XWPFDocument doc, int no, String text) {
        XWPFParagraph p = paragraph(doc, no + ". " + text, 28, true, ParagraphAlignment.LEFT, "1F3864", 360, 120);
        p.setKeepNext(true);
    }

    private void subHeading(XWPFDocument doc, String text) {
        XWPFParagraph p = paragraph(doc, text, 22, true, ParagraphAlignment.LEFT, null, 200, 80);
        p.setKeepNext(true);
    }

    private void note(XWPFDocument doc, String text) {
        paragraph(doc, text, 18, false, ParagraphAlignment.LEFT, NOTE_COLOR, 60, 60);
    }

    private void spacer(XWPFDocument doc) {
        paragraph(doc, "", 8, false, ParagraphAlignment.LEFT, null, 0, 0);
    }

    /** XML 1.0에서 허용되지 않는 문자와 줄바꿈 제거, 너무 긴 문자열 절단 */
    static String clean(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(Math.min(s.length(), MAX_CELL_CHARS + 1));
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\r' || c == '\n' || c == '\t') {
                sb.append(' ');
            } else if (c == 0x9 || c == 0xA || c == 0xD
                    || (c >= 0x20 && c <= 0xD7FF)
                    || (c >= 0xE000 && c <= 0xFFFD)
                    || Character.isHighSurrogate(c) || Character.isLowSurrogate(c)) {
                sb.append(c);
            }
        }
        String r = sb.toString();
        if (r.length() > MAX_CELL_CHARS) {
            r = r.substring(0, MAX_CELL_CHARS) + "…";
        }
        return r;
    }

    private static String num(long n) {
        return String.format("%,d", n);
    }

    // ── 표 ────────────────────────────────────────────────────

    /** 고정 레이아웃 표를 만든다. 첫 행은 머리글(shaded, 페이지마다 반복) */
    private XWPFTable table(XWPFDocument doc, int[] widths, String[] headers, List<String[]> rows,
                            ParagraphAlignment[] aligns) {
        int cols = widths.length;
        XWPFTable t = doc.createTable(rows.size() + 1, cols);
        CTTbl ct = t.getCTTbl();
        CTTblPr pr = ct.getTblPr() != null ? ct.getTblPr() : ct.addNewTblPr();

        int total = 0;
        for (int w : widths) {
            total += w;
        }
        CTTblWidth tw = pr.isSetTblW() ? pr.getTblW() : pr.addNewTblW();
        tw.setType(STTblWidth.DXA);
        tw.setW(BigInteger.valueOf(total));
        if (!pr.isSetTblLayout()) {
            pr.addNewTblLayout();
        }
        pr.getTblLayout().setType(STTblLayoutType.FIXED);

        CTTblGrid grid = ct.getTblGrid() != null ? ct.getTblGrid() : ct.addNewTblGrid();
        while (grid.sizeOfGridColArray() > 0) {
            grid.removeGridCol(0);
        }
        for (int w : widths) {
            grid.addNewGridCol().setW(BigInteger.valueOf(w));
        }

        t.setInsideHBorder(org.apache.poi.xwpf.usermodel.XWPFTable.XWPFBorderType.SINGLE, 4, 0, "999999");
        t.setInsideVBorder(org.apache.poi.xwpf.usermodel.XWPFTable.XWPFBorderType.SINGLE, 4, 0, "999999");
        t.setTopBorder(org.apache.poi.xwpf.usermodel.XWPFTable.XWPFBorderType.SINGLE, 4, 0, "999999");
        t.setBottomBorder(org.apache.poi.xwpf.usermodel.XWPFTable.XWPFBorderType.SINGLE, 4, 0, "999999");
        t.setLeftBorder(org.apache.poi.xwpf.usermodel.XWPFTable.XWPFBorderType.SINGLE, 4, 0, "999999");
        t.setRightBorder(org.apache.poi.xwpf.usermodel.XWPFTable.XWPFBorderType.SINGLE, 4, 0, "999999");

        XWPFTableRow head = t.getRow(0);
        if (head.getCtRow().getTrPr() == null) {
            head.getCtRow().addNewTrPr();
        }
        head.getCtRow().getTrPr().addNewTblHeader();
        for (int c = 0; c < cols; c++) {
            fill(head.getCell(c), headers[c], true, ParagraphAlignment.CENTER, widths[c], HEADER_FILL);
        }
        for (int r = 0; r < rows.size(); r++) {
            XWPFTableRow row = t.getRow(r + 1);
            String[] v = rows.get(r);
            for (int c = 0; c < cols; c++) {
                fill(row.getCell(c), c < v.length ? v[c] : "", false, aligns[c], widths[c], null);
            }
        }
        return t;
    }

    private void fill(XWPFTableCell cell, String text, boolean bold, ParagraphAlignment align, int width,
                      String fill) {
        CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        CTTblWidth w = tcPr.isSetTcW() ? tcPr.getTcW() : tcPr.addNewTcW();
        w.setType(STTblWidth.DXA);
        w.setW(BigInteger.valueOf(width));
        cell.setVerticalAlignment(XWPFVertAlign.CENTER);
        if (fill != null) {
            cell.setColor(fill);
        }
        XWPFParagraph p = cell.getParagraphs().get(0);
        p.setAlignment(align);
        p.setSpacingBefore(40);
        p.setSpacingAfter(40);
        XWPFRun r = p.createRun();
        font(r, 18, bold, null);
        r.setText(clean(text));
    }

    private static final ParagraphAlignment L = ParagraphAlignment.LEFT;
    private static final ParagraphAlignment C = ParagraphAlignment.CENTER;
    private static final ParagraphAlignment R = ParagraphAlignment.RIGHT;

    /** 구분 / 건수 형태의 집계 표 (맨 아래 합계 행 포함) */
    private void countTable(XWPFDocument doc, String title, String labelHeader, List<ReportCountVO> counts) {
        subHeading(doc, title);
        if (counts == null || counts.isEmpty()) {
            note(doc, "해당 기간에 데이터가 없습니다.");
            return;
        }
        List<String[]> rows = new ArrayList<>();
        long sum = 0;
        for (ReportCountVO c : counts) {
            rows.add(new String[]{c.getLabel() == null ? "(없음)" : c.getLabel(), num(c.getCount())});
            sum += c.getCount();
        }
        rows.add(new String[]{"합계", num(sum)});
        table(doc, new int[]{BODY_W - 2400, 2400}, new String[]{labelHeader, "건수"}, rows,
                new ParagraphAlignment[]{L, R});
        spacer(doc);
    }

    /** 상세 목록이 최대 행 수로 잘렸을 때만 안내 문구를 표시한다. (총건수는 집계표 합계에 있다) */
    private void detailNote(XWPFDocument doc, ReportData d, int total, int shown) {
        if (total > shown) {
            note(doc, "전체 " + num(total) + "건 중 최근 " + num(shown) + "건만 표시합니다. (최대 "
                    + num(d.getMaxDetailRows()) + "건)");
        }
    }

    // ── 본문 ──────────────────────────────────────────────────

    private void writeTitle(XWPFDocument doc, ReportData d) {
        paragraph(doc, d.getReportName(), 40, true, ParagraphAlignment.CENTER, "1F3864", 240, 120);
        paragraph(doc, "수자원통신위성 수문자료 수집 시스템 운영 리포트", 22, false, ParagraphAlignment.CENTER,
                NOTE_COLOR, 0, 240);
    }

    private void writeInfo(XWPFDocument doc, ReportData d) {
        String[][] kv = {
                {"리포트 ID", d.getReportNo() == null ? "-" : d.getReportNo()},
                {"조회 기간", d.getPeriodFrom().format(DATE_FMT) + " ~ " + d.getPeriodTo().format(DATE_FMT)},
                {"데이터 유형", String.join(", ", d.getDataTypeLabels())},
                {"생성자", d.getGeneratedBy() == null ? "-" : d.getGeneratedBy()},
                {"생성 일시", d.getGeneratedAt().format(DT_FMT)},
                {"생성 방식", d.getGenerateTypeLabel()},
        };
        int[] widths = {2000, BODY_W - 2000};
        XWPFTable t = doc.createTable(kv.length, 2);
        CTTbl ct = t.getCTTbl();
        CTTblPr pr = ct.getTblPr() != null ? ct.getTblPr() : ct.addNewTblPr();
        CTTblWidth tw = pr.isSetTblW() ? pr.getTblW() : pr.addNewTblW();
        tw.setType(STTblWidth.DXA);
        tw.setW(BigInteger.valueOf(BODY_W));
        if (!pr.isSetTblLayout()) {
            pr.addNewTblLayout();
        }
        pr.getTblLayout().setType(STTblLayoutType.FIXED);
        CTTblGrid grid = ct.getTblGrid() != null ? ct.getTblGrid() : ct.addNewTblGrid();
        while (grid.sizeOfGridColArray() > 0) {
            grid.removeGridCol(0);
        }
        for (int w : widths) {
            grid.addNewGridCol().setW(BigInteger.valueOf(w));
        }
        for (int i = 0; i < kv.length; i++) {
            fill(t.getRow(i).getCell(0), kv[i][0], true, C, widths[0], LABEL_FILL);
            fill(t.getRow(i).getCell(1), kv[i][1], false, L, widths[1], null);
        }
        spacer(doc);
    }

    private void writeOverview(XWPFDocument doc, ReportData d, int no) {
        heading(doc, no, "개요");
        List<String[]> rows = new ArrayList<>();
        if (d.getLog() != null) {
            rows.add(new String[]{"로그", num(d.getLog().getTotal())});
        }
        if (d.getEvent() != null) {
            rows.add(new String[]{"이벤트", num(d.getEvent().getTotal())});
        }
        if (d.getAlarm() != null) {
            rows.add(new String[]{"알람", num(d.getAlarm().getTotal())});
        }
        if (d.getReceive() != null) {
            rows.add(new String[]{"수신데이터", num(d.getReceive().getTotal())});
        }
        table(doc, new int[]{BODY_W - 2400, 2400}, new String[]{"데이터 유형", "건수"}, rows,
                new ParagraphAlignment[]{L, R});
        spacer(doc);
    }

    private void writeLog(XWPFDocument doc, ReportData d, int no) {
        ReportData.LogSection s = d.getLog();
        heading(doc, no, "로그");
        if (s.getTotal() == 0) {
            note(doc, "해당 기간에 데이터가 없습니다.");
            return;
        }
        countTable(doc, "레벨별 건수", "레벨", s.getLevelCounts());
        countTable(doc, "장비별 건수", "장비", s.getDeviceCounts());

        subHeading(doc, "상세 목록");
        detailNote(doc, d, s.getTotal(), s.getDetails().size());
        List<String[]> rows = new ArrayList<>();
        for (ReportLogRowVO r : s.getDetails()) {
            rows.add(new String[]{r.getRegDt(), r.getDeviceName(), r.getLevel(), r.getMessage()});
        }
        table(doc, new int[]{2000, 1700, 1000, BODY_W - 4700}, new String[]{"일시", "장비", "레벨", "내용"}, rows,
                new ParagraphAlignment[]{C, L, C, L});
    }

    private void writeEvent(XWPFDocument doc, ReportData d, int no) {
        ReportData.EventSection s = d.getEvent();
        heading(doc, no, "이벤트");
        if (s.getTotal() == 0) {
            note(doc, "해당 기간에 데이터가 없습니다.");
            return;
        }
        countTable(doc, "유형별 건수", "이벤트 유형", s.getTypeCounts());

        subHeading(doc, "상세 목록");
        detailNote(doc, d, s.getTotal(), s.getDetails().size());
        List<String[]> rows = new ArrayList<>();
        for (ReportEventRowVO r : s.getDetails()) {
            rows.add(new String[]{r.getRegDt(), r.getUserId(), r.getEventType(), r.getMessage()});
        }
        table(doc, new int[]{2000, 1500, 1800, BODY_W - 5300}, new String[]{"일시", "사용자", "유형", "내용"}, rows,
                new ParagraphAlignment[]{C, L, L, L});
    }

    private void writeAlarm(XWPFDocument doc, ReportData d, int no) {
        ReportData.AlarmSection s = d.getAlarm();
        heading(doc, no, "알람");
        if (s.getTotal() == 0) {
            note(doc, "해당 기간에 데이터가 없습니다.");
            return;
        }
        countTable(doc, "등급별 건수", "알람 등급", s.getLevelCounts());
        countTable(doc, "유형별 건수", "알람 유형", s.getTypeCounts());

        subHeading(doc, "상세 목록");
        detailNote(doc, d, s.getTotal(), s.getDetails().size());
        List<String[]> rows = new ArrayList<>();
        for (ReportAlarmRowVO r : s.getDetails()) {
            rows.add(new String[]{r.getOccurredAt(), r.getDeviceName(), r.getAlarmType(), r.getAlarmLevel(),
                    r.getAlarmMessage()});
        }
        table(doc, new int[]{2000, 1600, 1400, 900, BODY_W - 5900},
                new String[]{"발생일시", "장비", "유형", "등급", "내용"}, rows,
                new ParagraphAlignment[]{C, L, L, C, L});
    }

    private void writeReceive(XWPFDocument doc, ReportData d, int no) {
        ReportData.ReceiveSection s = d.getReceive();
        heading(doc, no, "수신데이터");
        if (s.getTotal() == 0) {
            note(doc, "해당 기간에 수신된 데이터가 없습니다.");
        }
        countTable(doc, "대역별 수신 건수", "대역", s.getBandCounts());
        countTable(doc, "L대역(DCS) 상태별 건수", "상태", s.getDcsStatusCounts());
        countTable(doc, "Ka대역 상태별 건수", "상태", s.getKaStatusCounts());

        subHeading(doc, "일자별 수신 건수");
        if (s.getDailyCounts() == null || s.getDailyCounts().isEmpty()) {
            note(doc, "해당 기간에 데이터가 없습니다.");
        } else {
            List<String[]> rows = new ArrayList<>();
            for (ReportDailyReceiveVO r : s.getDailyCounts()) {
                rows.add(new String[]{r.getDay(), num(r.getDcsCount()), num(r.getKaCount()),
                        num(r.getDcsCount() + r.getKaCount())});
            }
            table(doc, new int[]{BODY_W - 3 * 1800, 1800, 1800, 1800},
                    new String[]{"일자", "L대역(DCS)", "Ka대역", "합계"}, rows, new ParagraphAlignment[]{C, R, R, R});
            spacer(doc);
        }

        subHeading(doc, "장비별 수신 현황");
        if (s.getDeviceCounts() == null || s.getDeviceCounts().isEmpty()) {
            note(doc, "해당 기간에 데이터가 없습니다.");
        } else {
            List<String[]> rows = new ArrayList<>();
            for (ReportDeviceReceiveVO r : s.getDeviceCounts()) {
                rows.add(new String[]{r.getDeviceName(), r.getDeviceType(), num(r.getReceiveCount()),
                        r.getLastReceivedAt() == null ? "-" : r.getLastReceivedAt()});
            }
            table(doc, new int[]{BODY_W - 1500 - 1500 - 2400, 1500, 1500, 2400},
                    new String[]{"장비", "유형", "수신 건수", "마지막 수신"}, rows,
                    new ParagraphAlignment[]{L, C, R, C});
            spacer(doc);
        }

        if (s.getTotal() > 0) {
            subHeading(doc, "상세 목록");
            detailNote(doc, d, s.getTotal(), s.getDetails().size());
            List<String[]> rows = new ArrayList<>();
            for (ReportReceiveRowVO r : s.getDetails()) {
                rows.add(new String[]{r.getReceivedAt(), r.getDeviceName(), r.getBand(), r.getStatus(),
                        r.getPayload()});
            }
            table(doc, new int[]{2000, 1500, 800, 1000, BODY_W - 5300},
                    new String[]{"수신일시", "장비", "대역", "상태", "수신 내용"}, rows,
                    new ParagraphAlignment[]{C, L, C, C, L});
        }
    }
}
