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

    void insertAlarmCheck(@Param("alarmId") Long alarmId, @Param("userId") String userId);
}
