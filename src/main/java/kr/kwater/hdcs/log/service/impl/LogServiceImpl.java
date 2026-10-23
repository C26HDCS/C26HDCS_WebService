package kr.kwater.hdcs.log.service.impl;

import lombok.RequiredArgsConstructor;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.kwater.hdcs.log.dao.LogDAO;
import kr.kwater.hdcs.log.service.LogService;
import kr.kwater.hdcs.log.service.LogLevel;
import kr.kwater.hdcs.log.vo.LogVO;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LogServiceImpl extends EgovAbstractServiceImpl implements LogService {

    private final LogDAO logDAO;

    @Override
    public List<LogVO> getLogList(LogVO vo) throws Exception {
        return logDAO.selectLogList(vo);
    }

    @Override
    public int getLogCount(LogVO vo) throws Exception {
        return logDAO.selectLogCount(vo);
    }

    @Override
    @Transactional
    public long saveLog(Long deviceId, LogLevel level, String message) {
        if (deviceId == null || deviceId <= 0) {
            throw new IllegalArgumentException("deviceId must be positive");
        }
        if (level == null) {
            throw new IllegalArgumentException("level is required");
        }
        validateText(message, "message", 1000);

        LogVO vo = new LogVO();
        vo.setDeviceId(deviceId);
        vo.setLevel(level.name());
        vo.setMessage(message);
        return savedId(logDAO.insertLog(vo), vo);
    }

    @Override
    @Transactional
    public long saveEvent(String userId, String eventType, String message) {
        validateText(userId, "userId", 50);
        validateText(eventType, "eventType", 30);
        validateText(message, "message", 1000);

        LogVO vo = new LogVO();
        vo.setUserId(userId);
        vo.setLevel(eventType);
        vo.setMessage(message);
        return savedId(logDAO.insertEvent(vo), vo);
    }

    @Override
    @Transactional
    public long saveSystemEvent(String eventType, String message) {
        validateText(eventType, "eventType", 30);
        validateText(message, "message", 1000);

        LogVO vo = new LogVO();
        vo.setLevel(eventType);
        vo.setMessage(message);
        return savedId(logDAO.insertSystemEvent(vo), vo);
    }

    private static void validateText(String value, String name, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        if (value.codePointCount(0, value.length()) > maxLength) {
            throw new IllegalArgumentException(name + " must not exceed " + maxLength + " characters");
        }
    }

    private static long savedId(int inserted, LogVO vo) {
        if (inserted != 1 || vo.getId() == null) {
            throw new IllegalStateException("Log insert did not return a generated ID");
        }
        return vo.getId();
    }
}
