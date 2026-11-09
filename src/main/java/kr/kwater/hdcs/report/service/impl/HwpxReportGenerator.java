package kr.kwater.hdcs.report.service.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Component;

import kr.kwater.hdcs.report.service.ReportGenerator;
import kr.kwater.hdcs.report.vo.ReportAlarmRowVO;
import kr.kwater.hdcs.report.vo.ReportCountVO;
import kr.kwater.hdcs.report.vo.ReportData;
import kr.kwater.hdcs.report.vo.ReportDailyReceiveVO;
import kr.kwater.hdcs.report.vo.ReportDeviceReceiveVO;
import kr.kwater.hdcs.report.vo.ReportEventRowVO;
import kr.kwater.hdcs.report.vo.ReportLogRowVO;
import kr.kwater.hdcs.report.vo.ReportReceiveRowVO;

/**
 * 한글(.hwpx) 리포트 생성기.
 *
 * HWPX 는 XML 파일들을 ZIP 으로 묶은 개방형 문서 형식(OWPML)이다. 외부 라이브러리 없이 XML 을 직접 만들어 담는다.
 * 본문 구성은 기존 Word 리포트(WordReportGenerator)와 같다.
 * 홍수통제소 양식(template)이 확정되면 이 클래스의 본문 작성부(write*)만 교체하면 된다.
 *
 * 길이 단위는 HWPUNIT(1/7200 인치 = 1/100 pt). Word 의 twip(1/20 pt)에 5 를 곱한 값과 같다.
 */
@Component
public class HwpxReportGenerator implements ReportGenerator {

    private static final String FONT = "맑은 고딕";

    private static final int PAGE_W = 59528;           // A4 세로 210mm
    private static final int PAGE_H = 84188;           // A4 세로 297mm
    private static final int MARGIN = 5669;            // 2cm
    private static final int BODY_W = PAGE_W - MARGIN * 2;

    private static final String TITLE_COLOR = "1F3864";
    private static final String NOTE_COLOR = "666666";
    private static final String BLACK = "000000";

    /** borderFill ID: 1 = 쪽 테두리(없음), 2 = 문단 테두리(없음), 3 = 표 칸, 4 = 표 머리글(음영), 5 = 표 라벨(음영) */
    private static final int BF_CELL = 3;
    private static final int BF_HEAD = 4;
    private static final int BF_LABEL = 5;

    private static final String HEAD_FILL = "D9E2F3";
    private static final String LABEL_FILL = "F2F2F2";
    private static final String LINE_COLOR = "999999";

    private static final int MAX_CELL_CHARS = 200;

    private static final String NS_HEAD = "xmlns:hh=\"http://www.hancom.co.kr/hwpml/2011/head\""
            + " xmlns:hc=\"http://www.hancom.co.kr/hwpml/2011/core\""
            + " xmlns:hp=\"http://www.hancom.co.kr/hwpml/2011/paragraph\""
            + " xmlns:hs=\"http://www.hancom.co.kr/hwpml/2011/section\"";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String XML_DECL = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>";

    @Override
    public String getExtension() {
        return "hwpx";
    }

    @Override
    public void generate(ReportData data, OutputStream out) throws IOException {
        Doc doc = new Doc();

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

        doc.pack(out, clean(data.getReportName()));
    }

    // ════════════════════════════════════════════════════════════
    // 문서 상태(글자·문단 모양 등록부, 본문 XML 버퍼)와 HWPX 패키징
    // generate() 호출마다 새로 만들어지므로 싱글턴 빈에서도 안전하다.
    // ════════════════════════════════════════════════════════════

    static final class Doc {
        final StringBuilder body = new StringBuilder(64 * 1024);
        /** 글자 모양: key(크기|굵게|색) → id */
        final Map<String, Integer> charPrs = new LinkedHashMap<>();
        /** 문단 모양: key(정렬|앞간격|뒤간격|줄간격|다음과붙임) → id */
        final Map<String, Integer> paraPrs = new LinkedHashMap<>();
        int tableId = 1;
        boolean sectionWritten = false;

        Doc() {
            cp(1000, false, BLACK); // id 0: 기본 글자
            pp("LEFT", 0, 0, 130, false); // id 0: 기본 문단
        }

        int cp(int height, boolean bold, String color) {
            String key = height + "|" + bold + "|" + color;
            Integer id = charPrs.get(key);
            if (id == null) {
                id = charPrs.size();
                charPrs.put(key, id);
            }
            return id;
        }

        int pp(String align, int prev, int next, int linePercent, boolean keepNext) {
            String key = align + "|" + prev + "|" + next + "|" + linePercent + "|" + keepNext;
            Integer id = paraPrs.get(key);
            if (id == null) {
                id = paraPrs.size();
                paraPrs.put(key, id);
            }
            return id;
        }

        // ── 본문 ──

        /** 문단 하나. 첫 문단에는 쪽 설정(secPr)과 쪽 번호를 함께 싣는다. */
        void para(String text, int charPr, int paraPr) {
            body.append("<hp:p id=\"0\" paraPrIDRef=\"").append(paraPr)
                    .append("\" styleIDRef=\"0\" pageBreak=\"0\" columnBreak=\"0\" merged=\"0\">");
            if (!sectionWritten) {
                sectionWritten = true;
                body.append("<hp:run charPrIDRef=\"0\">").append(SEC_PR).append("</hp:run>");
            }
            body.append("<hp:run charPrIDRef=\"").append(charPr).append("\">");
            if (text == null || text.isEmpty()) {
                body.append("<hp:t/>");
            } else {
                body.append("<hp:t>").append(esc(text)).append("</hp:t>");
            }
            body.append("</hp:run></hp:p>");
        }

        void heading(int no, String text) {
            para(no + ". " + text, cp(1400, true, TITLE_COLOR), pp("LEFT", 1800, 600, 130, true));
        }

        void subHeading(String text) {
            para(text, cp(1100, true, BLACK), pp("LEFT", 1000, 400, 130, true));
        }

        void note(String text) {
            para(text, cp(900, false, NOTE_COLOR), pp("LEFT", 300, 300, 130, false));
        }

        void spacer() {
            para("", cp(800, false, BLACK), pp("LEFT", 0, 0, 100, false));
        }

        /**
         * 표. headers 가 null 이면 머리글 없이 만들고, labelCol 이면 첫 칸을 라벨(굵게·음영)로 표시한다.
         * widths 는 Word 와 같은 twip 단위(1/20pt)로 받아 HWPUNIT 으로 바꾼다.
         */
        void table(int[] widthsTw, String[] headers, List<String[]> rows, String[] aligns, boolean labelCol) {
            int cols = widthsTw.length;
            int[] w = new int[cols];
            int totalW = 0;
            for (int c = 0; c < cols; c++) {
                w[c] = widthsTw[c] * 5;
                totalW += w[c];
            }
            int rowCount = rows.size() + (headers != null ? 1 : 0);
            if (rowCount == 0) {
                return;
            }

            int cell9 = cp(900, false, BLACK);
            int cell9b = cp(900, true, BLACK);

            StringBuilder tb = new StringBuilder(rowCount * 256);
            int totalH = 0;
            int rowIdx = 0;
            if (headers != null) {
                int h = rowHeight(headers, w);
                totalH += h;
                tb.append("<hp:tr>");
                for (int c = 0; c < cols; c++) {
                    cell(tb, headers[c], c, rowIdx, w[c], h, BF_HEAD, cell9b, pp("CENTER", 0, 0, 130, false), true);
                }
                tb.append("</hp:tr>");
                rowIdx++;
            }
            for (String[] r : rows) {
                int h = rowHeight(r, w);
                totalH += h;
                tb.append("<hp:tr>");
                for (int c = 0; c < cols; c++) {
                    String v = c < r.length ? r[c] : "";
                    boolean label = labelCol && c == 0;
                    cell(tb, v, c, rowIdx, w[c], h,
                            label ? BF_LABEL : BF_CELL,
                            label ? cell9b : cell9,
                            pp(label ? "CENTER" : aligns[c], 0, 0, 130, false), false);
                }
                tb.append("</hp:tr>");
                rowIdx++;
            }

            body.append("<hp:p id=\"0\" paraPrIDRef=\"").append(pp("LEFT", 0, 0, 100, false))
                    .append("\" styleIDRef=\"0\" pageBreak=\"0\" columnBreak=\"0\" merged=\"0\">");
            if (!sectionWritten) {
                sectionWritten = true;
                body.append("<hp:run charPrIDRef=\"0\">").append(SEC_PR).append("</hp:run>");
            }
            body.append("<hp:run charPrIDRef=\"0\">");
            body.append("<hp:tbl id=\"").append(tableId++).append("\" zOrder=\"0\" numberingType=\"TABLE\"")
                    .append(" textWrap=\"TOP_AND_BOTTOM\" textFlow=\"BOTH_SIDES\" lock=\"0\" dropcapstyle=\"None\"")
                    .append(" pageBreak=\"CELL\" repeatHeader=\"").append(headers != null ? 1 : 0)
                    .append("\" rowCnt=\"").append(rowCount).append("\" colCnt=\"").append(cols)
                    .append("\" cellSpacing=\"0\" borderFillIDRef=\"").append(BF_CELL).append("\" noAdjust=\"0\">");
            body.append("<hp:sz width=\"").append(totalW).append("\" widthRelTo=\"ABSOLUTE\" height=\"")
                    .append(totalH).append("\" heightRelTo=\"ABSOLUTE\" protect=\"0\"/>");
            // 글자처럼 취급(treatAsChar=1)하면 표가 글자 한 개처럼 한 줄에 들어가서 쪽을 넘는 긴 표가 나뉘지 않고 잘린다.
            // 글자처럼 취급하지 않고(0) 본문 사이에 끼워 넣으면 쪽 경계에서 행 단위로 이어서 나뉜다.
            body.append("<hp:pos treatAsChar=\"0\" affectLSpacing=\"0\" flowWithText=\"1\" allowOverlap=\"0\"")
                    .append(" holdAnchorAndSO=\"0\" vertRelTo=\"PARA\" horzRelTo=\"COLUMN\" vertAlign=\"TOP\"")
                    .append(" horzAlign=\"LEFT\" vertOffset=\"0\" horzOffset=\"0\"/>");
            body.append("<hp:outMargin left=\"0\" right=\"0\" top=\"0\" bottom=\"0\"/>");
            body.append("<hp:inMargin left=\"").append(CELL_MARGIN_X).append("\" right=\"").append(CELL_MARGIN_X)
                    .append("\" top=\"").append(CELL_MARGIN_Y).append("\" bottom=\"").append(CELL_MARGIN_Y)
                    .append("\"/>");
            body.append(tb);
            body.append("</hp:tbl><hp:t/></hp:run></hp:p>");
        }

        private void cell(StringBuilder sb, String text, int col, int row, int width, int height,
                          int borderFill, int charPr, int paraPr, boolean header) {
            sb.append("<hp:tc name=\"\" header=\"").append(header ? 1 : 0)
                    .append("\" hasMargin=\"0\" protect=\"0\" editable=\"0\" dirty=\"0\" borderFillIDRef=\"")
                    .append(borderFill).append("\">");
            sb.append("<hp:subList id=\"\" textDirection=\"HORIZONTAL\" lineWrap=\"BREAK\" vertAlign=\"CENTER\"")
                    .append(" linkListIDRef=\"0\" linkListNextIDRef=\"0\" textWidth=\"0\" textHeight=\"0\"")
                    .append(" fieldName=\"\" metaTag=\"\">");
            sb.append("<hp:p id=\"0\" paraPrIDRef=\"").append(paraPr)
                    .append("\" styleIDRef=\"0\" pageBreak=\"0\" columnBreak=\"0\" merged=\"0\">")
                    .append("<hp:run charPrIDRef=\"").append(charPr).append("\">");
            String t = clean(text);
            if (t.isEmpty()) {
                sb.append("<hp:t/>");
            } else {
                sb.append("<hp:t>").append(esc(t)).append("</hp:t>");
            }
            sb.append("</hp:run></hp:p></hp:subList>");
            sb.append("<hp:cellAddr colAddr=\"").append(col).append("\" rowAddr=\"").append(row).append("\"/>");
            sb.append("<hp:cellSpan colSpan=\"1\" rowSpan=\"1\"/>");
            sb.append("<hp:cellSz width=\"").append(width).append("\" height=\"").append(height).append("\"/>");
            sb.append("<hp:cellMargin left=\"").append(CELL_MARGIN_X).append("\" right=\"").append(CELL_MARGIN_X)
                    .append("\" top=\"").append(CELL_MARGIN_Y).append("\" bottom=\"").append(CELL_MARGIN_Y)
                    .append("\"/>");
            sb.append("</hp:tc>");
        }

        // ── 패키징 ──

        void pack(OutputStream out, String title) throws IOException {
            String safeTitle = title == null ? "" : title;
            try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
                // mimetype 은 반드시 첫 항목이며 압축하지 않는다.
                byte[] mime = "application/hwp+zip".getBytes(StandardCharsets.UTF_8);
                ZipEntry m = new ZipEntry("mimetype");
                m.setMethod(ZipEntry.STORED);
                m.setSize(mime.length);
                m.setCompressedSize(mime.length);
                CRC32 crc = new CRC32();
                crc.update(mime);
                m.setCrc(crc.getValue());
                zip.putNextEntry(m);
                zip.write(mime);
                zip.closeEntry();

                put(zip, "version.xml", VERSION_XML);
                put(zip, "settings.xml", SETTINGS_XML);
                put(zip, "META-INF/container.xml", CONTAINER_XML);
                put(zip, "META-INF/manifest.xml", MANIFEST_XML);
                put(zip, "Contents/content.hpf", contentHpf(safeTitle));
                put(zip, "Contents/header.xml", headerXml());
                put(zip, "Contents/section0.xml", sectionXml());
                put(zip, "Preview/PrvText.txt", safeTitle);
            }
        }

        private static void put(ZipOutputStream zip, String name, String content) throws IOException {
            ZipEntry e = new ZipEntry(name);
            e.setMethod(ZipEntry.DEFLATED);
            zip.putNextEntry(e);
            zip.write(content.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        private String sectionXml() {
            return XML_DECL + "<hs:sec " + NS_HEAD + ">" + body + "</hs:sec>";
        }

        private String contentHpf(String title) {
            return XML_DECL
                    + "<opf:package xmlns:opf=\"http://www.idpf.org/2007/opf/\""
                    + " xmlns:dc=\"http://purl.org/dc/elements/1.1/\""
                    + " xmlns:hpf=\"http://www.hancom.co.kr/schema/2011/hpf\" version=\"\" unique-identifier=\"\" id=\"\">"
                    + "<opf:metadata><opf:title>" + esc(title) + "</opf:title><opf:language>ko</opf:language></opf:metadata>"
                    + "<opf:manifest>"
                    + "<opf:item id=\"header\" href=\"Contents/header.xml\" media-type=\"application/xml\"/>"
                    + "<opf:item id=\"section0\" href=\"Contents/section0.xml\" media-type=\"application/xml\"/>"
                    + "<opf:item id=\"settings\" href=\"settings.xml\" media-type=\"application/xml\"/>"
                    + "</opf:manifest>"
                    + "<opf:spine><opf:itemref idref=\"header\" linear=\"yes\"/><opf:itemref idref=\"section0\" linear=\"yes\"/></opf:spine>"
                    + "</opf:package>";
        }

        /** 글꼴·테두리·글자 모양·문단 모양·스타일 등 문서 공통 서식 */
        private String headerXml() {
            StringBuilder sb = new StringBuilder(8 * 1024);
            sb.append(XML_DECL).append("<hh:head ").append(NS_HEAD).append(" version=\"1.4\" secCnt=\"1\">");
            sb.append("<hh:beginNum page=\"1\" footnote=\"1\" endnote=\"1\" pic=\"1\" tbl=\"1\" equation=\"1\"/>");
            sb.append("<hh:refList>");

            // 글꼴: 모든 언어 그룹에 같은 글꼴을 지정한다.
            sb.append("<hh:fontfaces itemCnt=\"7\">");
            for (String lang : new String[]{"HANGUL", "LATIN", "HANJA", "JAPANESE", "OTHER", "SYMBOL", "USER"}) {
                sb.append("<hh:fontface lang=\"").append(lang).append("\" fontCnt=\"1\">")
                        .append("<hh:font id=\"0\" face=\"").append(FONT).append("\" type=\"TTF\" isEmbedded=\"0\">")
                        .append("<hh:typeInfo familyType=\"FCAT_GOTHIC\" weight=\"6\" proportion=\"4\" contrast=\"0\"")
                        .append(" strokeVariation=\"1\" armStyle=\"1\" letterform=\"1\" midline=\"1\" xHeight=\"1\"/>")
                        .append("</hh:font></hh:fontface>");
            }
            sb.append("</hh:fontfaces>");

            // 테두리/배경
            sb.append("<hh:borderFills itemCnt=\"5\">");
            borderFill(sb, 1, null, false);
            borderFill(sb, 2, null, false);
            borderFill(sb, BF_CELL, null, true);
            borderFill(sb, BF_HEAD, HEAD_FILL, true);
            borderFill(sb, BF_LABEL, LABEL_FILL, true);
            sb.append("</hh:borderFills>");

            // 글자 모양
            sb.append("<hh:charProperties itemCnt=\"").append(charPrs.size()).append("\">");
            for (Map.Entry<String, Integer> e : charPrs.entrySet()) {
                String[] k = e.getKey().split("\\|");
                sb.append("<hh:charPr id=\"").append(e.getValue()).append("\" height=\"").append(k[0])
                        .append("\" textColor=\"#").append(k[2])
                        .append("\" shadeColor=\"none\" useFontSpace=\"0\" useKerning=\"0\" symMark=\"NONE\" borderFillIDRef=\"2\">")
                        .append("<hh:fontRef hangul=\"0\" latin=\"0\" hanja=\"0\" japanese=\"0\" other=\"0\" symbol=\"0\" user=\"0\"/>")
                        .append("<hh:ratio hangul=\"100\" latin=\"100\" hanja=\"100\" japanese=\"100\" other=\"100\" symbol=\"100\" user=\"100\"/>")
                        .append("<hh:spacing hangul=\"0\" latin=\"0\" hanja=\"0\" japanese=\"0\" other=\"0\" symbol=\"0\" user=\"0\"/>")
                        .append("<hh:relSz hangul=\"100\" latin=\"100\" hanja=\"100\" japanese=\"100\" other=\"100\" symbol=\"100\" user=\"100\"/>")
                        .append("<hh:offset hangul=\"0\" latin=\"0\" hanja=\"0\" japanese=\"0\" other=\"0\" symbol=\"0\" user=\"0\"/>");
                if (Boolean.parseBoolean(k[1])) {
                    sb.append("<hh:bold/>");
                }
                sb.append("<hh:underline type=\"NONE\" shape=\"SOLID\" color=\"#000000\"/>")
                        .append("<hh:strikeout shape=\"NONE\" color=\"#000000\"/>")
                        .append("<hh:outline type=\"NONE\"/>")
                        .append("<hh:shadow type=\"NONE\" color=\"#B2B2B2\" offsetX=\"10\" offsetY=\"10\"/>")
                        .append("</hh:charPr>");
            }
            sb.append("</hh:charProperties>");

            sb.append("<hh:tabProperties itemCnt=\"1\"><hh:tabPr id=\"0\" autoTabLeft=\"0\" autoTabRight=\"0\"/></hh:tabProperties>");

            // 개요 번호(문서에 필수로 한 벌 있어야 한다)
            sb.append("<hh:numberings itemCnt=\"1\"><hh:numbering id=\"1\" start=\"0\">");
            for (int lv = 1; lv <= 7; lv++) {
                sb.append("<hh:paraHead start=\"1\" level=\"").append(lv)
                        .append("\" align=\"LEFT\" useInstWidth=\"1\" autoIndent=\"1\" widthAdjust=\"0\"")
                        .append(" textOffsetType=\"PERCENT\" textOffset=\"50\" numFormat=\"DIGIT\"")
                        .append(" charPrIDRef=\"4294967295\" checkable=\"0\">^").append(lv).append(".</hh:paraHead>");
            }
            sb.append("</hh:numbering></hh:numberings>");

            // 문단 모양
            sb.append("<hh:paraProperties itemCnt=\"").append(paraPrs.size()).append("\">");
            for (Map.Entry<String, Integer> e : paraPrs.entrySet()) {
                String[] k = e.getKey().split("\\|");
                sb.append("<hh:paraPr id=\"").append(e.getValue())
                        .append("\" tabPrIDRef=\"0\" condense=\"0\" fontLineHeight=\"0\" snapToGrid=\"0\"")
                        .append(" suppressLineNumbers=\"0\" checked=\"0\">")
                        .append("<hh:align horizontal=\"").append(k[0]).append("\" vertical=\"BASELINE\"/>")
                        .append("<hh:heading type=\"NONE\" idRef=\"0\" level=\"0\"/>")
                        .append("<hh:breakSetting breakLatinWord=\"KEEP_WORD\" breakNonLatinWord=\"KEEP_WORD\"")
                        .append(" widowOrphan=\"0\" keepWithNext=\"").append(Boolean.parseBoolean(k[4]) ? 1 : 0)
                        .append("\" keepLines=\"0\" pageBreakBefore=\"0\" lineWrap=\"BREAK\"/>")
                        .append("<hh:autoSpacing eAsianEng=\"0\" eAsianNum=\"0\"/>")
                        .append("<hh:margin><hc:intent value=\"0\" unit=\"HWPUNIT\"/><hc:left value=\"0\" unit=\"HWPUNIT\"/>")
                        .append("<hc:right value=\"0\" unit=\"HWPUNIT\"/><hc:prev value=\"").append(k[1])
                        .append("\" unit=\"HWPUNIT\"/><hc:next value=\"").append(k[2]).append("\" unit=\"HWPUNIT\"/></hh:margin>")
                        .append("<hh:lineSpacing type=\"PERCENT\" value=\"").append(k[3]).append("\" unit=\"HWPUNIT\"/>")
                        .append("<hh:border borderFillIDRef=\"2\" offsetLeft=\"0\" offsetRight=\"0\" offsetTop=\"0\"")
                        .append(" offsetBottom=\"0\" connect=\"0\" ignoreMargin=\"0\"/>")
                        .append("</hh:paraPr>");
            }
            sb.append("</hh:paraProperties>");

            sb.append("<hh:styles itemCnt=\"1\"><hh:style id=\"0\" type=\"PARA\" name=\"바탕글\" engName=\"Normal\"")
                    .append(" paraPrIDRef=\"0\" charPrIDRef=\"0\" nextStyleIDRef=\"0\" langID=\"1042\" lockForm=\"0\"/></hh:styles>");

            sb.append("</hh:refList>");
            sb.append("<hh:compatibleDocument targetProgram=\"HWP201X\"><hh:layoutCompatibility/></hh:compatibleDocument>");
            sb.append("<hh:docOption><hh:linkinfo path=\"\" pageInherit=\"0\" footnoteInherit=\"0\"/></hh:docOption>");
            sb.append("</hh:head>");
            return sb.toString();
        }

        private static void borderFill(StringBuilder sb, int id, String fill, boolean line) {
            String type = line ? "SOLID" : "NONE";
            String width = line ? "0.12 mm" : "0.1 mm";
            String color = line ? "#" + LINE_COLOR : "#000000";
            sb.append("<hh:borderFill id=\"").append(id)
                    .append("\" threeD=\"0\" shadow=\"0\" centerLine=\"NONE\" breakCellSeparateLine=\"0\">")
                    .append("<hh:slash type=\"NONE\" Crooked=\"0\" isCounter=\"0\"/>")
                    .append("<hh:backSlash type=\"NONE\" Crooked=\"0\" isCounter=\"0\"/>");
            for (String side : new String[]{"leftBorder", "rightBorder", "topBorder", "bottomBorder"}) {
                sb.append("<hh:").append(side).append(" type=\"").append(type).append("\" width=\"").append(width)
                        .append("\" color=\"").append(color).append("\"/>");
            }
            sb.append("<hh:diagonal type=\"SOLID\" width=\"0.1 mm\" color=\"#000000\"/>");
            if (fill != null) {
                sb.append("<hc:fillBrush><hc:winBrush faceColor=\"#").append(fill)
                        .append("\" hatchColor=\"#FF000000\" alpha=\"0\"/></hc:fillBrush>");
            }
            sb.append("</hh:borderFill>");
        }
    }

    // ── 고정 XML 조각 ────────────────────────────────────────────

    /** 첫 문단에 싣는 쪽 설정: A4 세로, 여백 2cm, 아래 가운데 쪽 번호("- 1 -") */
    private static final String SEC_PR =
            "<hp:secPr id=\"\" textDirection=\"HORIZONTAL\" spaceColumns=\"1134\" tabStop=\"8000\" tabStopVal=\"4000\""
            + " tabStopUnit=\"HWPUNIT\" outlineShapeIDRef=\"1\" memoShapeIDRef=\"0\" textVerticalWidthHead=\"0\" masterPageCnt=\"0\">"
            + "<hp:grid lineGrid=\"0\" charGrid=\"0\" wonggojiFormat=\"0\"/>"
            + "<hp:startNum pageStartsOn=\"BOTH\" page=\"0\" pic=\"0\" tbl=\"0\" equation=\"0\"/>"
            + "<hp:visibility hideFirstHeader=\"0\" hideFirstFooter=\"0\" hideFirstMasterPage=\"0\" border=\"SHOW_ALL\""
            + " fill=\"SHOW_ALL\" hideFirstPageNum=\"0\" hideFirstEmptyLine=\"0\" showLineNumber=\"0\"/>"
            + "<hp:lineNumberShape restartType=\"0\" countBy=\"0\" distance=\"0\" startNumber=\"0\"/>"
            + "<hp:pagePr landscape=\"WIDELY\" width=\"" + PAGE_W + "\" height=\"" + PAGE_H + "\" gutterType=\"LEFT_ONLY\">"
            + "<hp:margin header=\"3402\" footer=\"3402\" gutter=\"0\" left=\"" + MARGIN + "\" right=\"" + MARGIN
            + "\" top=\"" + MARGIN + "\" bottom=\"" + MARGIN + "\"/></hp:pagePr>"
            + "<hp:footNotePr><hp:autoNumFormat type=\"DIGIT\" userChar=\"\" prefixChar=\"\" suffixChar=\")\" supscript=\"0\"/>"
            + "<hp:noteLine length=\"-1\" type=\"SOLID\" width=\"0.12 mm\" color=\"#000000\"/>"
            + "<hp:noteSpacing betweenNotes=\"283\" belowLine=\"567\" aboveLine=\"850\"/>"
            + "<hp:numbering type=\"CONTINUOUS\" newNum=\"1\"/><hp:placement place=\"EACH_COLUMN\" beneathText=\"0\"/></hp:footNotePr>"
            + "<hp:endNotePr><hp:autoNumFormat type=\"DIGIT\" userChar=\"\" prefixChar=\"\" suffixChar=\")\" supscript=\"0\"/>"
            + "<hp:noteLine length=\"14692344\" type=\"SOLID\" width=\"0.12 mm\" color=\"#000000\"/>"
            + "<hp:noteSpacing betweenNotes=\"0\" belowLine=\"567\" aboveLine=\"850\"/>"
            + "<hp:numbering type=\"CONTINUOUS\" newNum=\"1\"/><hp:placement place=\"END_OF_DOCUMENT\" beneathText=\"0\"/></hp:endNotePr>"
            + "<hp:pageBorderFill type=\"BOTH\" borderFillIDRef=\"1\" textBorder=\"PAPER\" headerInside=\"0\" footerInside=\"0\" fillArea=\"PAPER\">"
            + "<hp:offset left=\"1417\" right=\"1417\" top=\"1417\" bottom=\"1417\"/></hp:pageBorderFill>"
            + "<hp:pageBorderFill type=\"EVEN\" borderFillIDRef=\"1\" textBorder=\"PAPER\" headerInside=\"0\" footerInside=\"0\" fillArea=\"PAPER\">"
            + "<hp:offset left=\"1417\" right=\"1417\" top=\"1417\" bottom=\"1417\"/></hp:pageBorderFill>"
            + "<hp:pageBorderFill type=\"ODD\" borderFillIDRef=\"1\" textBorder=\"PAPER\" headerInside=\"0\" footerInside=\"0\" fillArea=\"PAPER\">"
            + "<hp:offset left=\"1417\" right=\"1417\" top=\"1417\" bottom=\"1417\"/></hp:pageBorderFill>"
            + "</hp:secPr>"
            + "<hp:ctrl><hp:colPr id=\"\" type=\"NEWSPAPER\" layout=\"LEFT\" colCount=\"1\" sameSz=\"1\" sameGap=\"0\"/></hp:ctrl>"
            + "<hp:ctrl><hp:pageNum pos=\"BOTTOM_CENTER\" formatType=\"DIGIT\" sideChar=\"-\"/></hp:ctrl>";

    private static final String VERSION_XML = XML_DECL
            + "<hv:HCFVersion xmlns:hv=\"http://www.hancom.co.kr/hwpml/2011/version\" tagetApplication=\"WORDPROCESSOR\""
            + " major=\"5\" minor=\"0\" micro=\"5\" buildNumber=\"0\" os=\"1\" xmlVersion=\"1.4\""
            + " application=\"Hancom Office Hangul\" appVersion=\"9, 1, 1, 5656 WIN32LEWindows_Unknown_Version\"/>";

    private static final String SETTINGS_XML = XML_DECL
            + "<ha:HWPApplicationSetting xmlns:ha=\"http://www.hancom.co.kr/hwpml/2011/app\""
            + " xmlns:config=\"urn:oasis:names:tc:opendocument:xmlns:config:1.0\">"
            + "<ha:CaretPosition listIDRef=\"0\" paraIDRef=\"0\" pos=\"0\"/></ha:HWPApplicationSetting>";

    private static final String CONTAINER_XML = XML_DECL
            + "<ocf:container xmlns:ocf=\"urn:oasis:names:tc:opendocument:xmlns:container\""
            + " xmlns:hpf=\"http://www.hancom.co.kr/schema/2011/hpf\"><ocf:rootfiles>"
            + "<ocf:rootfile full-path=\"Contents/content.hpf\" media-type=\"application/hwpml-package+xml\"/>"
            + "<ocf:rootfile full-path=\"Preview/PrvText.txt\" media-type=\"text/plain\"/>"
            + "</ocf:rootfiles></ocf:container>";

    private static final String MANIFEST_XML = XML_DECL
            + "<odf:manifest xmlns:odf=\"urn:oasis:names:tc:opendocument:xmlns:manifest:1.0\"/>";

    // ── 표 크기 계산 ──────────────────────────────────────────────

    private static final int CELL_MARGIN_X = 510;
    private static final int CELL_MARGIN_Y = 200;
    /** 9pt 글자 한 줄(줄간격 130%)이 차지하는 높이. 여유를 둔 추정값 */
    private static final int LINE_H = 1250;

    /** 표 한 행의 높이 추정: 칸마다 글자 수로 줄 수를 계산해 가장 큰 값을 쓴다. 한글이 열 때 내용에 맞춰 다시 계산한다. */
    private static int rowHeight(String[] cells, int[] widths) {
        int lines = 1;
        for (int c = 0; c < widths.length; c++) {
            String t = clean(c < cells.length ? cells[c] : "");
            int textW = 0;
            for (int i = 0; i < t.length(); i++) {
                textW += t.charAt(i) >= 0x80 ? 900 : 500;
            }
            int avail = Math.max(900, widths[c] - CELL_MARGIN_X * 2);
            lines = Math.max(lines, (textW + avail - 1) / avail);
        }
        return lines * LINE_H + CELL_MARGIN_Y * 2;
    }

    // ── 문자열 처리 ──────────────────────────────────────────────

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
            } else if ((c >= 0x20 && c <= 0xD7FF)
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

    private static String esc(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String num(long n) {
        return String.format("%,d", n);
    }

    // ════════════════════════════════════════════════════════════
    // 본문 (Word 리포트와 같은 구성)
    // ════════════════════════════════════════════════════════════

    private static final String L = "LEFT";
    private static final String C = "CENTER";
    private static final String R = "RIGHT";

    private void writeTitle(Doc doc, ReportData d) {
        doc.para(clean(d.getReportName()), doc.cp(2000, true, TITLE_COLOR), doc.pp(C, 1200, 600, 130, false));
        doc.para("수자원통신위성 수문자료 수집 시스템 운영 리포트", doc.cp(1100, false, NOTE_COLOR),
                doc.pp(C, 0, 1200, 130, false));
    }

    private void writeInfo(Doc doc, ReportData d) {
        List<String[]> kv = new ArrayList<>();
        kv.add(new String[]{"리포트 ID", d.getReportNo() == null ? "-" : d.getReportNo()});
        kv.add(new String[]{"조회 기간",
                d.getPeriodFrom().format(DATE_FMT) + " ~ " + d.getPeriodTo().format(DATE_FMT)});
        kv.add(new String[]{"데이터 유형", String.join(", ", d.getDataTypeLabels())});
        kv.add(new String[]{"생성자", d.getGeneratedBy() == null ? "-" : d.getGeneratedBy()});
        kv.add(new String[]{"생성 일시", d.getGeneratedAt().format(DT_FMT)});
        kv.add(new String[]{"생성 방식", d.getGenerateTypeLabel()});
        int bodyTw = BODY_W / 5;
        doc.table(new int[]{2000, bodyTw - 2000}, null, kv, new String[]{L, L}, true);
        doc.spacer();
    }

    private void writeOverview(Doc doc, ReportData d, int no) {
        doc.heading(no, "개요");
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
        int bodyTw = BODY_W / 5;
        doc.table(new int[]{bodyTw - 2400, 2400}, new String[]{"데이터 유형", "건수"}, rows, new String[]{L, R}, false);
        doc.spacer();
    }

    /** 구분 / 건수 형태의 집계 표 (맨 아래 합계 행 포함) */
    private void countTable(Doc doc, String title, String labelHeader, List<ReportCountVO> counts) {
        doc.subHeading(title);
        if (counts == null || counts.isEmpty()) {
            doc.note("해당 기간에 데이터가 없습니다.");
            return;
        }
        List<String[]> rows = new ArrayList<>();
        long sum = 0;
        for (ReportCountVO c : counts) {
            rows.add(new String[]{c.getLabel() == null ? "(없음)" : c.getLabel(), num(c.getCount())});
            sum += c.getCount();
        }
        rows.add(new String[]{"합계", num(sum)});
        int bodyTw = BODY_W / 5;
        doc.table(new int[]{bodyTw - 2400, 2400}, new String[]{labelHeader, "건수"}, rows, new String[]{L, R}, false);
        doc.spacer();
    }

    /** 상세 목록이 최대 행 수로 잘렸을 때만 안내 문구를 표시한다. (총건수는 집계표 합계에 있다) */
    private void detailNote(Doc doc, ReportData d, int total, int shown) {
        if (total > shown) {
            doc.note("전체 " + num(total) + "건 중 최근 " + num(shown) + "건만 표시합니다. (최대 "
                    + num(d.getMaxDetailRows()) + "건)");
        }
    }

    private void writeLog(Doc doc, ReportData d, int no) {
        ReportData.LogSection s = d.getLog();
        doc.heading(no, "로그");
        if (s.getTotal() == 0) {
            doc.note("해당 기간에 데이터가 없습니다.");
            return;
        }
        countTable(doc, "레벨별 건수", "레벨", s.getLevelCounts());
        countTable(doc, "장비별 건수", "장비", s.getDeviceCounts());

        doc.subHeading("상세 목록");
        detailNote(doc, d, s.getTotal(), s.getDetails().size());
        List<String[]> rows = new ArrayList<>();
        for (ReportLogRowVO r : s.getDetails()) {
            rows.add(new String[]{r.getRegDt(), r.getDeviceName(), r.getLevel(), r.getMessage()});
        }
        int bodyTw = BODY_W / 5;
        doc.table(new int[]{2000, 1700, 1000, bodyTw - 4700}, new String[]{"일시", "장비", "레벨", "내용"}, rows,
                new String[]{C, L, C, L}, false);
    }

    private void writeEvent(Doc doc, ReportData d, int no) {
        ReportData.EventSection s = d.getEvent();
        doc.heading(no, "이벤트");
        if (s.getTotal() == 0) {
            doc.note("해당 기간에 데이터가 없습니다.");
            return;
        }
        countTable(doc, "유형별 건수", "이벤트 유형", s.getTypeCounts());

        doc.subHeading("상세 목록");
        detailNote(doc, d, s.getTotal(), s.getDetails().size());
        List<String[]> rows = new ArrayList<>();
        for (ReportEventRowVO r : s.getDetails()) {
            rows.add(new String[]{r.getRegDt(), r.getUserId(), r.getEventType(), r.getMessage()});
        }
        int bodyTw = BODY_W / 5;
        doc.table(new int[]{2000, 1500, 1800, bodyTw - 5300}, new String[]{"일시", "사용자", "유형", "내용"}, rows,
                new String[]{C, L, L, L}, false);
    }

    private void writeAlarm(Doc doc, ReportData d, int no) {
        ReportData.AlarmSection s = d.getAlarm();
        doc.heading(no, "알람");
        if (s.getTotal() == 0) {
            doc.note("해당 기간에 데이터가 없습니다.");
            return;
        }
        countTable(doc, "등급별 건수", "알람 등급", s.getLevelCounts());
        countTable(doc, "유형별 건수", "알람 유형", s.getTypeCounts());

        doc.subHeading("상세 목록");
        detailNote(doc, d, s.getTotal(), s.getDetails().size());
        List<String[]> rows = new ArrayList<>();
        for (ReportAlarmRowVO r : s.getDetails()) {
            rows.add(new String[]{r.getOccurredAt(), r.getDeviceName(), r.getAlarmType(), r.getAlarmLevel(),
                    r.getAlarmMessage()});
        }
        int bodyTw = BODY_W / 5;
        doc.table(new int[]{2000, 1600, 1400, 900, bodyTw - 5900},
                new String[]{"발생일시", "장비", "유형", "등급", "내용"}, rows, new String[]{C, L, L, C, L}, false);
    }

    private void writeReceive(Doc doc, ReportData d, int no) {
        ReportData.ReceiveSection s = d.getReceive();
        doc.heading(no, "수신데이터");
        if (s.getTotal() == 0) {
            doc.note("해당 기간에 수신된 데이터가 없습니다.");
        }
        countTable(doc, "대역별 수신 건수", "대역", s.getBandCounts());
        countTable(doc, "L대역(DCS) 상태별 건수", "상태", s.getDcsStatusCounts());
        countTable(doc, "Ka대역 상태별 건수", "상태", s.getKaStatusCounts());

        int bodyTw = BODY_W / 5;

        doc.subHeading("일자별 수신 건수");
        if (s.getDailyCounts() == null || s.getDailyCounts().isEmpty()) {
            doc.note("해당 기간에 데이터가 없습니다.");
        } else {
            List<String[]> rows = new ArrayList<>();
            for (ReportDailyReceiveVO r : s.getDailyCounts()) {
                rows.add(new String[]{r.getDay(), num(r.getDcsCount()), num(r.getKaCount()),
                        num(r.getDcsCount() + r.getKaCount())});
            }
            doc.table(new int[]{bodyTw - 3 * 1800, 1800, 1800, 1800},
                    new String[]{"일자", "L대역(DCS)", "Ka대역", "합계"}, rows, new String[]{C, R, R, R}, false);
            doc.spacer();
        }

        doc.subHeading("장비별 수신 현황");
        if (s.getDeviceCounts() == null || s.getDeviceCounts().isEmpty()) {
            doc.note("해당 기간에 데이터가 없습니다.");
        } else {
            List<String[]> rows = new ArrayList<>();
            for (ReportDeviceReceiveVO r : s.getDeviceCounts()) {
                rows.add(new String[]{r.getDeviceName(), r.getDeviceType(), num(r.getReceiveCount()),
                        r.getLastReceivedAt() == null ? "-" : r.getLastReceivedAt()});
            }
            doc.table(new int[]{bodyTw - 1500 - 1500 - 2400, 1500, 1500, 2400},
                    new String[]{"장비", "유형", "수신 건수", "마지막 수신"}, rows, new String[]{L, C, R, C}, false);
            doc.spacer();
        }

        if (s.getTotal() > 0) {
            doc.subHeading("상세 목록");
            detailNote(doc, d, s.getTotal(), s.getDetails().size());
            List<String[]> rows = new ArrayList<>();
            for (ReportReceiveRowVO r : s.getDetails()) {
                rows.add(new String[]{r.getReceivedAt(), r.getDeviceName(), r.getBand(), r.getStatus(),
                        r.getPayload()});
            }
            doc.table(new int[]{2000, 1500, 800, 1000, bodyTw - 5300},
                    new String[]{"수신일시", "장비", "대역", "상태", "수신 내용"}, rows,
                    new String[]{C, L, C, C, L}, false);
        }
    }
}
