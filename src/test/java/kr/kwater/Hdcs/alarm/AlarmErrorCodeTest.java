package kr.kwater.Hdcs.alarm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.kwater.hdcs.alarm.vo.AlarmVO;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class AlarmErrorCodeTest {

    @ParameterizedTest
    @CsvSource({
            "'[DDS404] [DCS_DATA_STATUS] 최근 수신 없음', DDS404, '[DCS_DATA_STATUS] 최근 수신 없음'",
            "'[DCS_DATA_STATUS] 최근 수신 없음', DDS404, '[DCS_DATA_STATUS] 최근 수신 없음'",
            "'[Python:DCS_DATA_STATUS] FAULT: status=STALE', DDS404, '[DCS_DATA_STATUS] 수문 자료 수신 상태: 최근 수신 없음'",
            "'[STATUS_TIMEOUT] 장치 상태 갱신 지연', SYS408, '[STATUS_TIMEOUT] 장치 상태 갱신 지연'",
            "'[DEV500] 장치 오류', DEV500, '장치 오류'"
    })
    void exposesCodesAndDescriptionsInTheAlarmResponse(String stored, String code, String message) {
        AlarmVO alarm = new AlarmVO();
        alarm.setAlarmMessage(stored);

        JsonNode json = new ObjectMapper().valueToTree(alarm);
        assertThat(json.path("alarmMessage").asText()).isEqualTo(message);
        JsonNode error = json.path("errorCode");
        assertThat(error.path("code").asText()).isEqualTo(code);
        assertThat(error.path("description").asText()).isNotBlank();
        assertThat(error.path("check").asText()).isNotBlank();
    }

    @ParameterizedTest
    @ValueSource(strings = {"일반 알림", "[DCS_MONITOR] 수신 상태 변경", "[AUT401] 로그인 실패"})
    void leavesOtherNotificationsWithoutAnErrorCode(String message) {
        AlarmVO alarm = new AlarmVO();
        alarm.setAlarmMessage(message);

        assertThat(alarm.getErrorCode()).isNull();
        assertThat(alarm.getAlarmMessage()).isEqualTo(message);
    }
}
