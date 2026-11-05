package kr.kwater.hdcs.alarm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.stereotype.Service;

import kr.kwater.hdcs.alarm.dao.AlarmDAO;
import kr.kwater.hdcs.alarm.service.AlarmService;
import kr.kwater.hdcs.alarm.vo.AlarmVO;
import kr.kwater.hdcs.log.service.LogService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmServiceImpl extends EgovAbstractServiceImpl implements AlarmService {

    /** 조치완료 이벤트 유형 (TB_EVENT.EVENT_TYPE) */
    static final String EVENT_ALARM_ACTION = "ALARM_ACTION";

    private final AlarmDAO alarmDAO;
    private final LogService logService;

    @Override
    public List<AlarmVO> getAlarmList(AlarmVO vo) throws Exception {
        return alarmDAO.selectAlarmList(vo);
    }

    @Override
    public int getAlarmCount(AlarmVO vo) throws Exception {
        return alarmDAO.selectAlarmCount(vo);
    }

    @Override
    public int getUnreadCount(String currentUserId) throws Exception {
        return alarmDAO.selectUnreadCount(currentUserId);
    }

    @Override
    public boolean existsAlarm(Long alarmId) throws Exception {
        return alarmId != null && alarmDAO.selectAlarmExists(alarmId) > 0;
    }

    @Override
    public void checkAlarm(Long alarmId, String userId) throws Exception {
        alarmDAO.insertAlarmCheck(alarmId, userId);
    }

    @Override
    public AlarmVO getAlarm(Long alarmId) throws Exception {
        return alarmId == null ? null : alarmDAO.selectAlarmById(alarmId);
    }

    @Override
    public int actionAlarm(Long alarmId, String userId) throws Exception {
        // 조치는 내용을 확인한 뒤에 하는 것이므로, 읽음 기록이 없으면 같이 남긴다. (이미 있으면 그대로)
        alarmDAO.insertAlarmCheck(alarmId, userId);
        int changed = alarmDAO.updateAlarmAction(alarmId, userId);
        if (changed > 0) {
            recordActionEvent(alarmId, userId);
        }
        return changed;
    }

    /** 조치완료를 이벤트(TB_EVENT)로 남긴다. 이벤트 저장이 실패해도 조치완료 결과는 그대로 둔다. */
    private void recordActionEvent(Long alarmId, String userId) {
        try {
            AlarmVO alarm = alarmDAO.selectAlarmById(alarmId);
            String device = alarm == null ? "-" : (alarm.getDeviceName() != null ? alarm.getDeviceName()
                    : String.valueOf(alarm.getDeviceId()));
            String type = alarm == null ? "-" : alarm.getAlarmType();
            logService.saveEvent(userId, EVENT_ALARM_ACTION,
                    "알람 조치완료 [번호 " + alarmId + ", 장비 " + device + ", 유형 " + type + "]");
        } catch (RuntimeException e) {
            log.warn("알람 조치완료 이벤트 저장 실패: alarm={}, user={}", alarmId, userId, e);
        }
    }

    @Override
    public int checkAllAlarms(String userId) throws Exception {
        return alarmDAO.insertAllAlarmChecks(userId);
    }
}
