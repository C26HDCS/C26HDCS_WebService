package kr.kwater.hdcs.alarm.service;

import java.util.List;

import kr.kwater.hdcs.alarm.vo.AlarmVO;

public interface AlarmService {

    List<AlarmVO> getAlarmList(AlarmVO vo) throws Exception;

    int getAlarmCount(AlarmVO vo) throws Exception;

    int getUnreadCount(String currentUserId) throws Exception;

    void checkAlarm(Long alarmId, String userId) throws Exception;

    /** 이 사용자가 아직 확인하지 않은 알람을 모두 확인 처리하고, 처리한 건수를 돌려준다. */
    int checkAllAlarms(String userId) throws Exception;
}
