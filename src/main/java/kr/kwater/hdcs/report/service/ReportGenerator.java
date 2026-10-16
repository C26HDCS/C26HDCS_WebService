package kr.kwater.hdcs.report.service;

import kr.kwater.hdcs.report.vo.ReportData;

import java.io.IOException;
import java.io.OutputStream;

/** 리포트 파일 생성기. 형식(Word, PDF)별로 구현한다. */
public interface ReportGenerator {

    /** 파일 확장자 (점 제외) */
    String getExtension();

    void generate(ReportData data, OutputStream out) throws IOException;
}
