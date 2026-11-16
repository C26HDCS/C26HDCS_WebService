package kr.kwater.hdcs.alarm.service;

import java.util.List;

import kr.kwater.hdcs.alarm.vo.AlarmVO;

public interface AlarmService {

    List<AlarmVO> getAlarmList(AlarmVO vo) throws Exception;

    int getAlarmCount(AlarmVO vo) throws Exception;

    int getUnreadCount(String currentUserId) throws Exception;

    /** 해당 번호의 알람이 존재하는지 */
    boolean existsAlarm(Long alarmId) throws Exception;

    void checkAlarm(Long alarmId, String userId) throws Exception;

    /** 알람 한 건 (조치 상태 포함). 없으면 null */
    AlarmVO getAlarm(Long alarmId) throws Exception;

    /** 조치 메모 최대 글자 수 */
    int ACTION_MEMO_MAX_LENGTH = 200;

    /**
     * 알람을 조치완료 처리한다. 읽음 기록이 없으면 함께 남긴다.
     * 메모는 선택이며(null·공백이면 저장하지 않음) 최대 200자다.
     * 이미 조치완료된 알람이면 아무것도 바꾸지 않고 0, 이번에 처리했으면 1을 돌려준다.
     *
     * @throws IllegalArgumentException 메모가 200자를 넘는 경우
     */
    int actionAlarm(Long alarmId, String userId, String memo) throws Exception;

    /**
     * 조치 메모를 수정한다. 조치완료한 본인만 수정할 수 있고, 메모를 비우면 메모가 지워진다.
     *
     * @throws IllegalArgumentException 메모가 200자를 넘는 경우
     * @throws java.util.NoSuchElementException 알람이 없는 경우
     * @throws IllegalStateException 아직 조치완료되지 않은 알람인 경우
     * @throws org.springframework.security.access.AccessDeniedException 조치완료한 사람이 아닌 경우
     */
    void updateActionMemo(Long alarmId, String userId, String memo) throws Exception;

    /** 이 사용자가 아직 확인하지 않은 알람을 모두 확인 처리하고, 처리한 건수를 돌려준다. */
    int checkAllAlarms(String userId) throws Exception;
}
