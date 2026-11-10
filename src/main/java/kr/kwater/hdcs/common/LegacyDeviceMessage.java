package kr.kwater.hdcs.common;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts historical device messages for display while preserving stored history. */
public final class LegacyDeviceMessage {

    private static final Pattern PREFIX = Pattern.compile("^\\[Python:([A-Z0-9_]+)\\] *");
    private static final Map<String, String> STATUS_LABELS = Map.ofEntries(
            Map.entry("RUNNING", "동작 중"), Map.entry("STOPPED", "정지"),
            Map.entry("NEVER_STARTED", "시작 이력 없음"), Map.entry("UNRESPONSIVE", "응답 없음"),
            Map.entry("ERROR", "오류"), Map.entry("RECEIVING", "수신 중"),
            Map.entry("NO_DATA", "수신 이력 없음"), Map.entry("STALE", "최근 수신 없음"),
            Map.entry("INVALID_ONLY", "유효한 자료 없음"), Map.entry("NONE", "대기 없음"),
            Map.entry("WAITING", "대기 중"), Map.entry("DELAYED", "처리 지연"));

    private LegacyDeviceMessage() {}

    public static String display(String message) {
        if (message == null) {
            return null;
        }
        Matcher prefix = PREFIX.matcher(message);
        if (!prefix.find()) {
            return message;
        }
        String code = prefix.group(1);
        String detail = message.substring(prefix.end());
        boolean recovered = detail.startsWith("RECOVERED: ");
        String label = switch (code) {
            case "KA_MONITOR" -> "영상 수신기 " + status(detail, "receiver")
                    + " / 영상 " + status(detail, "data") + " / 처리 대기 상태: " + status(detail, "pending");
            case "KA_RECEIVER_STATUS" -> "영상 수신기 상태: " + status(detail, "receiver");
            case "KA_DATA_STATUS" -> "영상 수신 상태: " + status(detail, "data");
            case "DCS_MONITOR", "DCS_DATA_STATUS" -> "수문 자료 수신 상태: " + status(detail, "status");
            case "KA_PENDING_DELAY" -> recovered ? "영상 처리 지연이 해소되었습니다."
                    : "수신 대기 중인 영상의 처리가 지연되고 있습니다.";
            case "KA_RECEIVE_ERROR" -> recovered ? "영상 수신 오류가 해소되었습니다."
                    : "영상 수신 중 오류가 발생했습니다.";
            case "DCS_RECEIVE_INVALID" -> recovered ? "유효한 수문 자료가 다시 수신되고 있습니다."
                    : "유효하지 않은 수문 자료가 수신되었습니다.";
            case "KA_RECEIVE" -> "영상을 수신했습니다.";
            case "DCS_RECEIVE" -> detail.equals("Invalid frame stored") ? "수신한 수문 자료를 확인해 주세요."
                    : "수문 자료를 수신했습니다.";
            case "KA_RECEIVER", "DCS_RECEIVER" -> subject(code) + " 수신기가 "
                    + (detail.endsWith(" stopped") ? "정지했습니다." : "동작을 시작했습니다.");
            case "KA_SEND", "DCS_SEND" -> subject(code) + (detail.contains(" status=SENT ")
                    ? " 전송을 완료했습니다." : " 전송에 실패했습니다. 재시도할 예정입니다.");
            case "KA_SEND_ERROR", "DCS_SEND_ERROR" -> subject(code) + (recovered
                    ? " 전송이 정상화되었습니다." : " 전송에 실패했습니다.");
            case "STATUS_TIMEOUT" -> "장치 상태 정보가 제시간에 갱신되지 않았습니다.";
            case "STATUS_RECOVERED" -> "장치 상태 정보 갱신이 정상화되었습니다.";
            default -> code.endsWith("_RUNTIME") ? runtimeMessage(code, recovered)
                    : "장치 상태를 확인해 주세요.";
        };
        return "[" + code + "] " + label;
    }

    private static String status(String detail, String field) {
        Matcher matcher = Pattern.compile("(?:^| )" + field + "=([A-Z_]+)(?= |$)").matcher(detail);
        String label = "확인 필요";
        while (matcher.find()) {
            label = STATUS_LABELS.getOrDefault(matcher.group(1), "확인 필요");
        }
        return label;
    }

    private static String subject(String code) {
        return code.startsWith("KA_") ? "영상" : "수문 자료";
    }

    private static String runtimeMessage(String code, boolean recovered) {
        String component = switch (code.substring(code.indexOf('_') + 1)) {
            case "RECEIVER_RUNTIME" -> "수신기";
            case "SENDER_RUNTIME" -> "전송기";
            case "MONITOR_RUNTIME" -> "감시기";
            default -> "처리 프로그램";
        };
        return subject(code) + " " + component + (recovered
                ? " 동작이 정상화되었습니다." : "에 오류가 발생했습니다.");
    }
}
