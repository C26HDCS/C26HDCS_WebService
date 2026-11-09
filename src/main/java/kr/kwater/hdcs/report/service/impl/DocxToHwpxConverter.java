package kr.kwater.hdcs.report.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 한글 형식으로 바꾸기 전에 만든 예전 리포트(.docx)를 한글(.hwpx)로 변환한다.
 * Word 파일을 그대로 읽어서 제목, 문단, 표의 내용을 같은 모양으로 한글 파일에 옮긴다.
 * (WordReportGenerator 가 만든 구성을 기준으로 한다: 제목, 문단, 표)
 */
@Component
public class DocxToHwpxConverter {

    private static final String W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";

    /** docx 를 읽어 hwpxTarget 에 저장한다. (이미 있으면 덮어쓴다) */
    public void convert(Path docx, Path hwpxTarget) throws IOException {
        Document xml = readDocumentXml(docx);

        HwpxReportGenerator.Doc doc = new HwpxReportGenerator.Doc();
        String title = "";

        Element body = firstChild(xml.getDocumentElement(), "body");
        if (body == null) {
            throw new IOException("Word 파일의 본문을 찾을 수 없습니다.");
        }
        for (Node n = body.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() != Node.ELEMENT_NODE || !W.equals(n.getNamespaceURI())) {
                continue;
            }
            Element el = (Element) n;
            if ("p".equals(el.getLocalName())) {
                String text = paragraph(doc, el);
                if (title.isEmpty() && !text.isEmpty()) {
                    title = text;
                }
            } else if ("tbl".equals(el.getLocalName())) {
                table(doc, el);
            }
        }

        // 반쯤 쓰인 파일이 남지 않도록 같은 폴더에 임시 파일로 쓴 뒤 이름을 바꾼다.
        Path tmp = Files.createTempFile(hwpxTarget.getParent(), "report_", ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(tmp)) {
                doc.pack(out, title);
            }
            Files.move(tmp, hwpxTarget, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    // ── 문단 ────────────────────────────────────────────────────

    /** 문단 하나를 한글 문서에 옮기고, 문단의 글자를 돌려준다. */
    private String paragraph(HwpxReportGenerator.Doc doc, Element p) {
        String text = text(p);

        Element pPr = firstChild(p, "pPr");
        String align = align(pPr);
        int before = twipsToHwp(attrOf(firstChild(pPr, "spacing"), "before"), 0);
        int after = twipsToHwp(attrOf(firstChild(pPr, "spacing"), "after"), 0);

        Element firstRun = firstChild(p, "r");
        Element rPr = firstChild(firstRun, "rPr");
        int halfPt = intOr(attrOf(firstChild(rPr, "sz"), "val"), 20);
        boolean bold = isOn(firstChild(rPr, "b"));
        String color = color(attrOf(firstChild(rPr, "color"), "val"));

        if (text.isEmpty()) {
            doc.spacer();
            return "";
        }
        // 굵은 큰 글씨 왼쪽 정렬 문단은 소제목 성격이므로 다음 문단과 같은 쪽에 두도록 한다.
        boolean keepNext = bold && halfPt >= 22 && "LEFT".equals(align);
        doc.para(HwpxReportGenerator.clean(text), doc.cp(halfPt * 50, bold, color),
                doc.pp(align, before, after, 130, keepNext));
        return text;
    }

    // ── 표 ──────────────────────────────────────────────────────

    private void table(HwpxReportGenerator.Doc doc, Element tbl) {
        List<Element> rows = children(tbl, "tr");
        if (rows.isEmpty()) {
            return;
        }

        List<Integer> widths = new ArrayList<>();
        Element grid = firstChild(tbl, "tblGrid");
        for (Element gc : children(grid, "gridCol")) {
            widths.add(intOr(attrOf(gc, "w"), 2000));
        }
        int cols = widths.size();
        if (cols == 0) {
            cols = children(rows.get(0), "tc").size();
            for (int i = 0; i < cols; i++) {
                widths.add(9638 / Math.max(1, cols));
            }
        }
        int[] w = new int[cols];
        for (int i = 0; i < cols; i++) {
            w[i] = widths.get(i);
        }

        // 첫 행이 머리글(페이지마다 반복)인지, 첫 칸이 라벨(음영)인지 판단한다.
        boolean hasHeader = firstChild(firstChild(rows.get(0), "trPr"), "tblHeader") != null;
        List<Element> firstCells = children(rows.get(0), "tc");
        boolean labelCol = !hasHeader && !firstCells.isEmpty()
                && "F2F2F2".equalsIgnoreCase(attrOf(firstChild(firstChild(firstCells.get(0), "tcPr"), "shd"), "fill"));

        String[] headers = null;
        int dataStart = 0;
        if (hasHeader) {
            headers = cellTexts(firstCells, cols);
            dataStart = 1;
        }

        List<String[]> data = new ArrayList<>();
        for (int i = dataStart; i < rows.size(); i++) {
            data.add(cellTexts(children(rows.get(i), "tc"), cols));
        }

        // 칸별 정렬은 첫 데이터 행(없으면 머리글 행)의 문단 정렬을 따른다.
        String[] aligns = new String[cols];
        List<Element> alignCells = children(rows.get(Math.min(dataStart, rows.size() - 1)), "tc");
        for (int c = 0; c < cols; c++) {
            aligns[c] = c < alignCells.size() ? align(firstChild(firstChild(alignCells.get(c), "p"), "pPr")) : "LEFT";
        }

        doc.table(w, headers, data, aligns, labelCol);
    }

    private String[] cellTexts(List<Element> cells, int cols) {
        String[] out = new String[cols];
        for (int c = 0; c < cols; c++) {
            out[c] = c < cells.size() ? text(cells.get(c)) : "";
        }
        return out;
    }

    // ── XML 읽기 보조 ───────────────────────────────────────────

    private Document readDocumentXml(Path docx) throws IOException {
        try (ZipFile zip = new ZipFile(docx.toFile())) {
            ZipEntry entry = zip.getEntry("word/document.xml");
            if (entry == null) {
                throw new IOException("Word 파일이 올바르지 않습니다.");
            }
            try (InputStream in = zip.getInputStream(entry)) {
                DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
                f.setNamespaceAware(true);
                // 외부 개체 참조 등을 막는다.
                f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                f.setXIncludeAware(false);
                f.setExpandEntityReferences(false);
                DocumentBuilder b = f.newDocumentBuilder();
                return b.parse(in);
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException("Word 파일을 읽을 수 없습니다.", e);
            }
        }
    }

    /** 요소 안의 모든 글자(w:t)를 이어 붙인다. 문단이 여러 개면 공백으로 구분한다. */
    private static String text(Element el) {
        StringBuilder sb = new StringBuilder();
        collectText(el, sb);
        return sb.toString().trim();
    }

    private static void collectText(Node node, StringBuilder sb) {
        NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            if (n.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element e = (Element) n;
            if (W.equals(e.getNamespaceURI())) {
                String name = e.getLocalName();
                if ("t".equals(name)) {
                    sb.append(e.getTextContent());
                    continue;
                }
                if ("tab".equals(name) || "br".equals(name)) {
                    sb.append(' ');
                    continue;
                }
                if ("p".equals(name) && sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
                    sb.append(' ');
                }
            }
            collectText(e, sb);
        }
    }

    private static Element firstChild(Element parent, String localName) {
        if (parent == null) {
            return null;
        }
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE && W.equals(n.getNamespaceURI())
                    && localName.equals(n.getLocalName())) {
                return (Element) n;
            }
        }
        return null;
    }

    private static List<Element> children(Element parent, String localName) {
        List<Element> out = new ArrayList<>();
        if (parent == null) {
            return out;
        }
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE && W.equals(n.getNamespaceURI())
                    && localName.equals(n.getLocalName())) {
                out.add((Element) n);
            }
        }
        return out;
    }

    /** w: 속성 값 (없으면 null) */
    private static String attrOf(Element el, String name) {
        if (el == null) {
            return null;
        }
        String v = el.getAttributeNS(W, name);
        return v == null || v.isEmpty() ? null : v;
    }

    /** w:b 처럼 켜짐/꺼짐을 나타내는 요소: 있고 값이 false/0 이 아니면 켜짐 */
    private static boolean isOn(Element el) {
        if (el == null) {
            return false;
        }
        String v = attrOf(el, "val");
        return v == null || !("0".equals(v) || "false".equalsIgnoreCase(v) || "off".equalsIgnoreCase(v));
    }

    private static String align(Element pPr) {
        String jc = attrOf(firstChild(pPr, "jc"), "val");
        if ("center".equals(jc)) {
            return "CENTER";
        }
        if ("right".equals(jc) || "end".equals(jc)) {
            return "RIGHT";
        }
        return "LEFT";
    }

    private static String color(String v) {
        return v != null && v.matches("[0-9A-Fa-f]{6}") ? v.toUpperCase() : "000000";
    }

    private static int intOr(String v, int def) {
        try {
            return v == null ? def : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** Word 의 twip(1/20pt)를 한글의 HWPUNIT(1/100pt)으로 바꾼다. */
    private static int twipsToHwp(String v, int def) {
        int t = intOr(v, def);
        return Math.max(0, t) * 5;
    }
}
