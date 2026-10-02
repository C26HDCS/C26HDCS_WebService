package kr.kwater.hdcs.alarm.vo;

import kr.kwater.hdcs.common.vo.BaseVO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlarmVO extends BaseVO {

    private Long   id;
    private Long   deviceId;
    private String deviceName;
    private String alarmType;
    private String alarmLevel;
    private String alarmMessage;
    private String occurredAt;

    private String searchAlarmType;
    private String searchAlarmLevel;
    private String searchDateFrom;
    private String searchDateTo;

    private boolean checked;
    private String  currentUserId;
    private Integer limit;         // 헤더 알림 미리보기용 최대 건수 (없으면 전체 조회)
}
