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

    /** 현재 사용자가 아직 확인하지 않은 알람을 모두 확인 처리한다. 처리된 건수를 돌려준다. */
    int insertAllAlarmChecks(@Param("userId") String userId);

    void insertAlarmCheck(@Param("alarmId") Long alarmId, @Param("userId") String userId);

    /** 알람 한 건 조회 (조치 상태 포함) */
    AlarmVO selectAlarmById(@Param("alarmId") Long alarmId);

    /**
     * 아직 조치되지 않은 알람을 조치완료로 바꾸고, 메모(선택, 없으면 null)를 함께 저장한다.
     * 바뀐 행 수(이미 조치된 알람이면 0)를 돌려준다.
     */
    int updateAlarmAction(@Param("alarmId") Long alarmId, @Param("userId") String userId,
                          @Param("memo") String memo);

    /**
     * 조치 메모를 수정한다. 조치완료된 알람이고 조치완료한 사람 본인일 때만 바뀐다.
     * 바뀐 행 수(조건에 맞지 않으면 0)를 돌려준다. memo 가 null 이면 메모를 비운다.
     */
    int updateAlarmActionMemo(@Param("alarmId") Long alarmId, @Param("userId") String userId,
                              @Param("memo") String memo);

    /** 해당 번호의 알람이 있으면 1, 없으면 0 */
    int selectAlarmExists(@Param("alarmId") Long alarmId);
}
