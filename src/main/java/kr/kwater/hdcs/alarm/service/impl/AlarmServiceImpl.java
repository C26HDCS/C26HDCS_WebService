package kr.kwater.hdcs.alarm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.stereotype.Service;

import kr.kwater.hdcs.alarm.dao.AlarmDAO;
import kr.kwater.hdcs.alarm.service.AlarmService;
import kr.kwater.hdcs.alarm.vo.AlarmVO;
import kr.kwater.hdcs.log.service.LogService;

import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.NoSuchElementException;

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

    /** 메모 앞뒤 공백을 지우고, 비어 있으면 null(메모 없음)로 만든다. 200자를 넘으면 예외. */
    private static String normalizeMemo(String memo) {
        if (memo == null) {
            return null;
        }
        String trimmed = memo.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        // DB(char_length)와 같은 기준으로 글자 수를 센다. (이모지 같은 문자도 1자)
        if (trimmed.codePointCount(0, trimmed.length()) > ACTION_MEMO_MAX_LENGTH) {
            throw new IllegalArgumentException("메모는 " + ACTION_MEMO_MAX_LENGTH + "자까지 입력할 수 있습니다.");
        }
        return trimmed;
    }

    @Override
    public int actionAlarm(Long alarmId, String userId, String memo) throws Exception {
        String normalized = normalizeMemo(memo);
        // 조치는 내용을 확인한 뒤에 하는 것이므로, 읽음 기록이 없으면 같이 남긴다. (이미 있으면 그대로)
        alarmDAO.insertAlarmCheck(alarmId, userId);
        int changed = alarmDAO.updateAlarmAction(alarmId, userId, normalized);
        if (changed > 0) {
            recordActionEvent(alarmId, userId, normalized != null ? "조치완료(메모 포함)" : "조치완료");
        }
        return changed;
    }

    @Override
    public void updateActionMemo(Long alarmId, String userId, String memo) throws Exception {
        String normalized = normalizeMemo(memo);
        if (alarmDAO.updateAlarmActionMemo(alarmId, userId, normalized) > 0) {
            recordActionEvent(alarmId, userId, normalized != null ? "조치 메모 수정" : "조치 메모 삭제");
            return;
        }
        // 바뀐 행이 없으면 이유를 구분해서 알린다.
        AlarmVO alarm = alarmDAO.selectAlarmById(alarmId);
        if (alarm == null) {
            throw new NoSuchElementException("존재하지 않는 알람입니다.");
        }
        if (!"Y".equals(alarm.getActionYn())) {
            throw new IllegalStateException("조치완료된 알람에만 메모를 남길 수 있습니다.");
        }
        throw new AccessDeniedException("조치완료한 사용자만 메모를 수정할 수 있습니다.");
    }

    /** 조치완료·메모 수정을 이벤트(TB_EVENT)로 남긴다. 이벤트 저장이 실패해도 처리 결과는 그대로 둔다. (메모 내용은 남기지 않는다) */
    private void recordActionEvent(Long alarmId, String userId, String action) {
        try {
            AlarmVO alarm = alarmDAO.selectAlarmById(alarmId);
            String device = alarm == null ? "-" : (alarm.getDeviceName() != null ? alarm.getDeviceName()
                    : String.valueOf(alarm.getDeviceId()));
            String type = alarm == null ? "-" : alarm.getAlarmType();
            logService.saveEvent(userId, EVENT_ALARM_ACTION,
                    "알람 " + action + " [번호 " + alarmId + ", 장비 " + device + ", 유형 " + type + "]");
        } catch (RuntimeException e) {
            log.warn("알람 조치완료 이벤트 저장 실패: alarm={}, user={}", alarmId, userId, e);
        }
    }

    @Override
    public int checkAllAlarms(String userId) throws Exception {
        return alarmDAO.insertAllAlarmChecks(userId);
    }
}
