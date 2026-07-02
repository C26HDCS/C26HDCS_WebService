package kr.kwater.hdcs.alarm.service.impl;

import lombok.RequiredArgsConstructor;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.stereotype.Service;

import kr.kwater.hdcs.alarm.dao.AlarmDAO;
import kr.kwater.hdcs.alarm.service.AlarmService;
import kr.kwater.hdcs.alarm.vo.AlarmVO;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlarmServiceImpl extends EgovAbstractServiceImpl implements AlarmService {

    private final AlarmDAO alarmDAO;

    @Override
    public List<AlarmVO> getAlarmList(AlarmVO vo) throws Exception {
        return alarmDAO.selectAlarmList(vo);
    }

    @Override
    public int getAlarmCount(AlarmVO vo) throws Exception {
        return alarmDAO.selectAlarmCount(vo);
    }
}
