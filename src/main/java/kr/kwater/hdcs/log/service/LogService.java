package kr.kwater.hdcs.log.service;

import java.util.List;

import kr.kwater.hdcs.log.vo.LogVO;

public interface LogService {

    List<LogVO> getLogList(LogVO vo) throws Exception;

    int getLogCount(LogVO vo) throws Exception;

    /** Save a device log and return its generated ID. Joins the caller's transaction. */
    long saveLog(Long deviceId, LogLevel level, String message);

    /** Save a user event and return its generated ID. Joins the caller's transaction. */
    long saveEvent(String userId, String eventType, String message);

    /**
     * Save a system-initiated event (no signed-in user, e.g. scheduled jobs) and return its generated ID.
     * Stored with USER_ID NULL and ACTOR_USER_ID 'SYSTEM', so no TB_USER row is required.
     * Joins the caller's transaction.
     */
    long saveSystemEvent(String eventType, String message);
}
