package kr.kwater.hdcs.alarm.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.kwater.hdcs.alarm.vo.AlarmVO;

import java.util.List;

@Mapper
public interface AlarmDAO {

    List<AlarmVO> selectAlarmList(AlarmVO vo);

    int selectAlarmCount(AlarmVO vo);

    int selectUnreadCount(@Param("currentUserId") String currentUserId);

    /** 알람 테이블 변경 감지용 값 (알람 수 : 최대 ID : 확인 기록 수). 값이 달라지면 알림 개수를 다시 보낸다. */
    String selectAlarmSignature();

    void insertAlarmCheck(@Param("alarmId") Long alarmId, @Param("userId") String userId);
}
