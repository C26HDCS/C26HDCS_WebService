package kr.kwater.hdcs.log.service.impl;

import lombok.RequiredArgsConstructor;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.stereotype.Service;

import kr.kwater.hdcs.log.dao.LogDAO;
import kr.kwater.hdcs.log.service.LogService;
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
}
