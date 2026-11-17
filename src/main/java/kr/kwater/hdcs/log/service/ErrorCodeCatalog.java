package kr.kwater.hdcs.log.service;

import kr.kwater.hdcs.common.LegacyDeviceMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class ErrorCodeCatalog {

    public record Entry(String code, String group, String title, String description, String check,
                        List<String> sources) {}

    private static final Pattern MESSAGE_PREFIX = Pattern.compile("^\\[(?:Python:)?([A-Z0-9_]+)]");
    private static final Entry OTHER_LOG_ERROR = entry("DEV500", "시스템", "장치 로그 오류",
            "세부 오류 유형을 식별할 수 없는 장치 오류가 기록되었습니다.",
            "로그 상세 내용과 같은 시각의 장치 실행 로그를 확인하세요.");
    // Device codes are also written by C26HDCS_Python's hdcs_telemetry.ERROR_CODES.
    private static final List<Entry> ENTRIES = List.of(
            entry("DDS404", "DCS", "수문 자료 미수신",
                    "수신 이력이 없거나, 최근 수신이 없거나, 수신한 자료가 모두 유효하지 않습니다.",
                    "관측소 연결 상태, 마지막 수신 시각, 수신 자료의 유효성을 확인하세요.", "DCS_DATA_STATUS"),
            entry("DRC422", "DCS", "수문 자료 형식 오류",
                    "수신한 수문 자료를 정상적으로 해석할 수 없습니다.",
                    "송신 장비의 자료 형식과 프레임 검사 결과를 확인하세요.", "DCS_RECEIVE_INVALID"),
            entry("DTX502", "DCS", "수문 자료 전송 실패",
                    "수신한 수문 자료를 설정된 목적지에 전송하지 못했습니다.",
                    "목적지 연결, 응답 상태, 재시도 결과를 확인하세요.", "DCS_SEND", "DCS_SEND_ERROR"),
            entry("DRX501", "DCS", "수문 자료 수신기 실행 오류",
                    "수문 자료 수신기에서 예외가 발생했습니다.",
                    "수신기 프로세스와 실행 로그를 확인하세요.", "DCS_RECEIVER_RUNTIME"),
            entry("DTX501", "DCS", "수문 자료 전송기 실행 오류",
                    "수문 자료 전송기에서 예외가 발생했습니다.",
                    "전송기 프로세스, DB 연결, 실행 로그를 확인하세요.", "DCS_SENDER_RUNTIME"),
            entry("DRM501", "DCS", "수문 자료 감시기 실행 오류",
                    "수문 자료 감시 중 감시기에서 예외가 발생했습니다.",
                    "감시기 프로세스, DB 연결, 실행 로그를 확인하세요.", "DCS_MONITOR_RUNTIME"),
            entry("DCS501", "DCS", "수문 자료 처리 프로그램 실행 오류",
                    "수문 자료 처리 프로그램의 시작 또는 실행 중 예외가 발생했습니다.",
                    "프로그램 실행 상태와 시작·종료 시각의 로그를 확인하세요.", "DCS_PROCESS_RUNTIME"),
            entry("KDS404", "Ka", "영상 자료 미수신",
                    "영상 수신 이력이 없거나 설정된 시간 동안 최근 영상이 수신되지 않았습니다.",
                    "영상 송신 장비, 원본 폴더, 마지막 수신 시각을 확인하세요.", "KA_DATA_STATUS"),
            entry("KRX503", "Ka", "영상 수신기 비정상 상태",
                    "영상 수신기가 정지했거나, 시작 이력이 없거나, 응답하지 않거나, 오류 상태입니다.",
                    "영상 수신기 실행 상태와 마지막 상태 갱신 시각을 확인하세요.", "KA_RECEIVER_STATUS"),
            entry("KRX502", "Ka", "영상 수신 처리 오류",
                    "영상 수신 폴더에 접근하지 못했거나 영상 파일 처리 중 오류가 발생했습니다.",
                    "폴더 경로와 접근 권한, 파일 상태를 확인하세요.", "KA_RECEIVE_ERROR"),
            entry("KPD408", "Ka", "영상 처리 지연",
                    "수신 대기 중인 영상이 설정된 대기 시간을 초과했습니다.",
                    "대기 파일 수, 가장 오래된 파일, 수신기 처리 상태를 확인하세요.", "KA_PENDING_DELAY"),
            entry("KTX502", "Ka", "영상 전송 실패",
                    "수신한 영상을 설정된 목적지에 전송하지 못했습니다.",
                    "목적지 연결, 응답 상태, 재시도 결과를 확인하세요.", "KA_SEND", "KA_SEND_ERROR"),
            entry("KRX501", "Ka", "영상 수신기 실행 오류",
                    "영상 수신기에서 예외가 발생했습니다.",
                    "수신기 프로세스와 실행 로그를 확인하세요.", "KA_RECEIVER_RUNTIME"),
            entry("KTX501", "Ka", "영상 전송기 실행 오류",
                    "영상 전송기에서 예외가 발생했습니다.",
                    "전송기 프로세스, DB 연결, 실행 로그를 확인하세요.", "KA_SENDER_RUNTIME"),
            entry("KRM501", "Ka", "영상 감시기 실행 오류",
                    "영상 수신 상태를 감시하는 중 감시기에서 예외가 발생했습니다.",
                    "감시기 프로세스, DB 연결, 실행 로그를 확인하세요.", "KA_MONITOR_RUNTIME"),
            entry("KAP501", "Ka", "영상 처리 프로그램 실행 오류",
                    "영상 처리 프로그램의 시작 또는 실행 중 예외가 발생했습니다.",
                    "프로그램 실행 상태와 시작·종료 시각의 로그를 확인하세요.", "KA_PROCESS_RUNTIME"),
            entry("SYS408", "시스템", "장치 상태 갱신 시간 초과",
                    "예정된 시간 안에 장치 상태 정보가 갱신되지 않았습니다.",
                    "장치의 마지막 상태 갱신 시각과 연결 상태를 확인하세요.", "STATUS_TIMEOUT"),
            OTHER_LOG_ERROR,
            entry("AUT401", "이벤트", "로그인 실패",
                    "로그인 요청을 인증하지 못했습니다.",
                    "사용자 ID, 비밀번호 및 계정 상태를 확인하세요.", "LOGIN_FAIL"),
            entry("STG501", "이벤트", "파일 삭제 실패",
                    "저장장치에서 요청한 파일을 삭제하지 못했습니다.",
                    "이벤트 상세 내용과 파일 경로, 삭제 권한을 확인하세요.", "STORAGE_DELETE_FAIL"),
            entry("STG502", "이벤트", "저장공간 점검 실패",
                    "저장공간 자동 점검 또는 정리 작업 중 오류가 발생했습니다.",
                    "저장장치 연결 상태와 정리 작업 로그를 확인하세요.", "STORAGE_CLEANUP_FAIL"),
            entry("RPT501", "이벤트", "리포트 생성 실패",
                    "수동 또는 예약 리포트 생성 중 오류가 발생했습니다.",
                    "대상 기간의 자료, 이벤트 상세 내용, 생성 작업 로그를 확인하세요.", "REPORT_CREATE_FAIL")
    );

    private static final Map<String, Entry> BY_CODE = new HashMap<>();
    private static final Map<String, Entry> BY_OPERATION = new HashMap<>();
    private static final Map<String, Entry> BY_EVENT = new HashMap<>();

    static {
        for (Entry entry : ENTRIES) {
            BY_CODE.put(entry.code(), entry);
            Map<String, Entry> bySource = isEvent(entry) ? BY_EVENT : BY_OPERATION;
            for (String source : entry.sources()) {
                bySource.put(source, entry);
            }
        }
    }

    private ErrorCodeCatalog() {}

    public static List<Entry> entries() {
        return ENTRIES;
    }

    public static Entry forRecord(String category, String level, String message) {
        if ("EVENT".equals(category)) {
            return BY_EVENT.get(level);
        }
        if (!"LOG".equals(category) || !"ERROR".equals(level) || message == null) {
            return null;
        }
        Entry entry = forDeviceMessage(message);
        return entry != null ? entry : OTHER_LOG_ERROR;
    }

    public static Entry forDeviceMessage(String message) {
        if (message == null) {
            return null;
        }
        var matcher = MESSAGE_PREFIX.matcher(message);
        if (!matcher.find()) {
            return null;
        }
        String source = matcher.group(1);
        Entry storedCode = BY_CODE.get(source);
        return storedCode != null && !isEvent(storedCode)
                ? storedCode : BY_OPERATION.get(source);
    }

    public static String displayDeviceMessage(String message, Entry entry) {
        if (entry != null && message != null) {
            String prefix = "[" + entry.code() + "] ";
            if (message.startsWith(prefix)) {
                return message.substring(prefix.length());
            }
        }
        return LegacyDeviceMessage.display(message);
    }

    public static List<String> logSearchPrefixes(String keyword) {
        Entry entry = findCode(keyword);
        if (entry == OTHER_LOG_ERROR) {
            return ENTRIES.stream()
                    .filter(known -> !isEvent(known) && known != OTHER_LOG_ERROR)
                    .flatMap(known -> messagePrefixes(known).stream())
                    .toList();
        }
        return entry == null || isEvent(entry) ? List.of() : messagePrefixes(entry);
    }

    public static boolean otherLogSearch(String keyword) {
        return findCode(keyword) == OTHER_LOG_ERROR;
    }

    private static List<String> messagePrefixes(Entry entry) {
        var prefixes = new ArrayList<String>();
        prefixes.add("[" + entry.code() + "]");
        for (String source : entry.sources()) {
            prefixes.add("[" + source + "]");
            prefixes.add("[Python:" + source + "]");
        }
        return prefixes;
    }

    public static String eventSearchType(String keyword) {
        Entry entry = findCode(keyword);
        return entry != null && isEvent(entry) ? entry.sources().get(0) : null;
    }

    private static Entry findCode(String keyword) {
        return keyword == null ? null : BY_CODE.get(keyword.strip().toUpperCase(Locale.ROOT));
    }

    private static boolean isEvent(Entry entry) {
        return "이벤트".equals(entry.group());
    }

    private static Entry entry(String code, String group, String title, String description,
                               String check, String... sources) {
        return new Entry(code, group, title, description, check, List.of(sources));
    }
}
