package kr.kwater.hdcs.alarm.service;

import java.util.List;

import kr.kwater.hdcs.alarm.vo.AlarmVO;

public interface AlarmService {

    List<AlarmVO> getAlarmList(AlarmVO vo) throws Exception;

    int getAlarmCount(AlarmVO vo) throws Exception;
}
