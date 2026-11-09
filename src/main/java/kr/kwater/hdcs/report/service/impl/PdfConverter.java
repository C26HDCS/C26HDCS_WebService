package kr.kwater.hdcs.report.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import kr.kwater.hdcs.report.config.ReportProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * 리포트 파일을 PDF로 변환한다.
 * 기본은 LibreOffice로, 리포트를 만들 때 함께 저장해 둔 Word(.docx) 파일을 변환한다.
 * report.pdf-engine 을 rhwp 로 바꾸면 한글(.hwpx) 파일을 RHWP로 변환한다.
 * 외부 프로그램은 동시에 여러 개 실행하면 충돌할 수 있어서 변환은 한 번에 하나씩만 실행한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PdfConverter {

    private final ReportProperties props;
    private final Object lock = new Object();

    /**
     * pdfTarget 이 아직 없으면 source 를 변환해서 pdfTarget 위치에 저장한다. (이미 있으면 아무것도 하지 않는다)
     *
     * @throws IllegalStateException 변환 프로그램이 없거나 변환에 실패한 경우 (메시지는 화면에 표시된다)
     */
    public void convertIfAbsent(Path source, Path pdfTarget) throws IOException {
        synchronized (lock) {
            // 대기하는 동안 다른 요청이 이미 만들었을 수 있으므로 잠금 안에서 다시 확인한다.
            if (Files.isRegularFile(pdfTarget) && Files.size(pdfTarget) > 0) {
                return;
            }
            String sourceName = source.getFileName().toString();
            boolean legacyWord = sourceName.toLowerCase().endsWith(".docx");
            boolean viaWord = legacyWord || "libreoffice".equalsIgnoreCase(props.getPdfEngine());

            // 한글 리포트를 LibreOffice 로 변환할 때는 리포트 생성 시 같이 저장해 둔 Word 파일(같은 이름, 확장자 .docx)을 쓴다.
            Path wordSource = source;
            if (viaWord && !legacyWord) {
                int dot = sourceName.lastIndexOf('.');
                String base = dot > 0 ? sourceName.substring(0, dot) : sourceName;
                wordSource = source.resolveSibling(base + ".docx");
                if (!Files.isRegularFile(wordSource)) {
                    log.error("PDF 변환용 Word 파일이 없음: {}", wordSource);
                    throw new IllegalStateException("이 리포트는 PDF 변환용 파일이 없어 PDF를 만들 수 없습니다. 리포트를 다시 생성해 주세요.");
                }
            }

            Path work = Files.createTempDirectory("report_pdf_");
            try {
                Path generated = viaWord ? convertWithLibreOffice(wordSource, work) : convertWithRhwp(source, work);

                // 반쯤 쓰인 PDF가 남지 않도록 같은 폴더에 임시 파일로 복사한 뒤 이름을 바꾼다.
                Path tmp = Files.createTempFile(pdfTarget.getParent(), "report_", ".tmp");
                try {
                    Files.copy(generated, tmp, StandardCopyOption.REPLACE_EXISTING);
                    Files.move(tmp, pdfTarget, StandardCopyOption.REPLACE_EXISTING);
                } finally {
                    Files.deleteIfExists(tmp);
                }
                log.info("PDF 변환 완료: {} -> {}", source.getFileName(), pdfTarget.getFileName());
            } finally {
                deleteTree(work);
            }
        }
    }

    // ── RHWP (hwpx → pdf) ───────────────────────────────────────

    private Path convertWithRhwp(Path source, Path work) throws IOException {
        String exe = props.getRhwpPath();
        // 경로(구분자 포함)로 지정한 경우에만 파일 존재를 확인한다. 'rhwp' 처럼 이름만 있으면 PATH 에서 찾는다.
        if (exe.contains("/") || exe.contains("\\")) {
            if (!Files.isRegularFile(Paths.get(exe))) {
                log.error("RHWP 실행 파일을 찾을 수 없음: {}", exe);
                throw new IllegalStateException("PDF 변환 프로그램(RHWP)을 찾을 수 없습니다. 관리자에게 문의해 주세요.");
            }
        }

        Path out = work.resolve("out.pdf");
        List<String> cmd = new ArrayList<>();
        cmd.add(exe);
        cmd.add("export-pdf");
        cmd.add(source.toString());
        cmd.add("-o");
        cmd.add(out.toString());
        if (props.isRhwpTextAsPaths()) {
            cmd.add("--text-as-paths");
        }

        Path logFile = work.resolve("rhwp.log");
        int exit = run(cmd, logFile, source);

        if (exit != 0 || !Files.isRegularFile(out) || Files.size(out) == 0) {
            log.error("PDF 변환 실패(RHWP): exit={}, file={}, output={}", exit, source, readLog(logFile));
            throw new IllegalStateException("PDF 변환에 실패했습니다. 관리자에게 문의해 주세요.");
        }
        return out;
    }

    // ── LibreOffice (docx → pdf, 예전 Word 리포트) ───────────────

    private Path convertWithLibreOffice(Path docx, Path work) throws IOException {
        Path soffice = Paths.get(props.getSofficePath());
        if (!Files.isRegularFile(soffice)) {
            log.error("LibreOffice 실행 파일을 찾을 수 없음: {}", soffice);
            throw new IllegalStateException("PDF 변환 프로그램(LibreOffice)을 찾을 수 없습니다. 관리자에게 문의해 주세요.");
        }

        // LibreOffice 는 같은 사용자 프로필로 동시에 실행하면 충돌하므로 매 변환마다 임시 프로필 폴더를 따로 쓴다.
        Path profile = work.resolve("profile");
        Path out = work.resolve("out");
        Files.createDirectories(out);

        List<String> cmd = new ArrayList<>();
        cmd.add(soffice.toString());
        cmd.add("-env:UserInstallation=" + profile.toUri());
        cmd.add("--headless");
        cmd.add("--norestore");
        cmd.add("--nolockcheck");
        cmd.add("--convert-to");
        cmd.add("pdf");
        cmd.add("--outdir");
        cmd.add(out.toString());
        cmd.add(docx.toString());

        Path logFile = work.resolve("soffice.log");
        int exit = run(cmd, logFile, docx);

        String base = docx.getFileName().toString();
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            base = base.substring(0, dot);
        }
        Path generated = out.resolve(base + ".pdf");
        if (exit != 0 || !Files.isRegularFile(generated) || Files.size(generated) == 0) {
            log.error("PDF 변환 실패(LibreOffice): exit={}, file={}, output={}", exit, docx, readLog(logFile));
            throw new IllegalStateException("PDF 변환에 실패했습니다. 관리자에게 문의해 주세요.");
        }
        return generated;
    }

    // ── 공통 ────────────────────────────────────────────────────

    /** 외부 프로그램을 실행하고 종료 코드를 돌려준다. 제한 시간을 넘기면 강제 종료한다. */
    private int run(List<String> cmd, Path logFile, Path source) throws IOException {
        Process proc;
        try {
            proc = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .redirectOutput(logFile.toFile())
                    .start();
        } catch (IOException e) {
            log.error("PDF 변환 프로그램 실행 실패: {}", cmd.get(0), e);
            throw new IllegalStateException("PDF 변환 프로그램을 실행할 수 없습니다. 관리자에게 문의해 주세요.");
        }
        boolean finished;
        try {
            finished = proc.waitFor(props.getPdfTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            killTree(proc);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("PDF 변환이 중단되었습니다.");
        }
        if (!finished) {
            killTree(proc);
            log.error("PDF 변환 시간 초과({}초): {}", props.getPdfTimeoutSeconds(), source);
            throw new IllegalStateException("PDF 변환 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요.");
        }
        return proc.exitValue();
    }

    private static String readLog(Path logFile) {
        try {
            return Files.isRegularFile(logFile) ? new String(Files.readAllBytes(logFile), StandardCharsets.UTF_8) : "";
        } catch (IOException e) {
            return "";
        }
    }

    /** 변환 프로그램은 실제 작업을 자식 프로세스에서 하기도 하므로 자식까지 함께 종료한다. */
    private static void killTree(Process proc) {
        proc.descendants().forEach(child -> child.destroyForcibly());
        proc.destroyForcibly();
    }

    private static void deleteTree(Path dir) {
        try (Stream<Path> s = Files.walk(dir)) {
            s.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // 임시 폴더 정리 실패는 무시한다.
                }
            });
        } catch (IOException ignored) {
            // 임시 폴더 정리 실패는 무시한다.
        }
    }
}
