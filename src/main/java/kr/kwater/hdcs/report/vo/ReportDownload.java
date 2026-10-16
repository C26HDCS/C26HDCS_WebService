package kr.kwater.hdcs.report.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.file.Path;

/** 리포트 파일 다운로드 정보 (다운로드명 + 실제 저장 경로) */
@Getter
@AllArgsConstructor
public class ReportDownload {
    private final String fileName;
    private final Path path;
    private final long size;
}
