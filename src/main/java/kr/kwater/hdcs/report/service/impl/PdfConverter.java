package kr.kwater.hdcs.report.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import kr.kwater.hdcs.report.config.ReportProperties;

import java.io.IOException;
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
 * LibreOffice(headless)로 Word(.docx) 파일을 PDF로 변환한다.
 * LibreOffice는 같은 사용자 프로필로 동시에 실행하면 충돌하므로 변환은 한 번에 하나씩만 실행하고,
 * 매 변환마다 임시 프로필 폴더를 따로 만들어 사용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PdfConverter {

    private final ReportProperties props;
    private final Object lock = new Object();

    /**
     * pdfTarget 이 아직 없으면 docx 를 변환해서 pdfTarget 위치에 저장한다. (이미 있으면 아무것도 하지 않는다)
     *
     * @throws IllegalStateException 변환 프로그램이 없거나 변환에 실패한 경우 (메시지는 화면에 표시된다)
     */
    public void convertIfAbsent(Path docx, Path pdfTarget) throws IOException {
        synchronized (lock) {
            // 대기하는 동안 다른 요청이 이미 만들었을 수 있으므로 잠금 안에서 다시 확인한다.
            if (Files.isRegularFile(pdfTarget) && Files.size(pdfTarget) > 0) {
                return;
            }
            Path soffice = Paths.get(props.getSofficePath());
            if (!Files.isRegularFile(soffice)) {
                log.error("LibreOffice 실행 파일을 찾을 수 없음: {}", soffice);
                throw new IllegalStateException("PDF 변환 프로그램(LibreOffice)을 찾을 수 없습니다. 관리자에게 문의해 주세요.");
            }

            Path work = Files.createTempDirectory("report_pdf_");
            try {
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
                Process proc = new ProcessBuilder(cmd)
                        .redirectErrorStream(true)
                        .redirectOutput(logFile.toFile())
                        .start();
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
                    log.error("PDF 변환 시간 초과({}초): {}", props.getPdfTimeoutSeconds(), docx);
                    throw new IllegalStateException("PDF 변환 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요.");
                }

                String base = docx.getFileName().toString();
                int dot = base.lastIndexOf('.');
                if (dot > 0) {
                    base = base.substring(0, dot);
                }
                Path generated = out.resolve(base + ".pdf");
                if (proc.exitValue() != 0 || !Files.isRegularFile(generated) || Files.size(generated) == 0) {
                    String output = Files.isRegularFile(logFile)
                            ? new String(Files.readAllBytes(logFile), java.nio.charset.StandardCharsets.UTF_8) : "";
                    log.error("PDF 변환 실패: exit={}, file={}, output={}", proc.exitValue(), docx, output);
                    throw new IllegalStateException("PDF 변환에 실패했습니다. 관리자에게 문의해 주세요.");
                }

                // 반쯤 쓰인 PDF가 남지 않도록 같은 폴더에 임시 파일로 복사한 뒤 이름을 바꾼다.
                Path tmp = Files.createTempFile(pdfTarget.getParent(), "report_", ".tmp");
                try {
                    Files.copy(generated, tmp, StandardCopyOption.REPLACE_EXISTING);
                    Files.move(tmp, pdfTarget, StandardCopyOption.REPLACE_EXISTING);
                } finally {
                    Files.deleteIfExists(tmp);
                }
                log.info("PDF 변환 완료: {} -> {}", docx.getFileName(), pdfTarget.getFileName());
            } finally {
                deleteTree(work);
            }
        }
    }

    /** soffice 는 실제 작업을 자식 프로세스(soffice.bin)에서 하므로 자식까지 함께 종료한다. */
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
