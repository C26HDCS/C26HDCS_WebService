package kr.kwater.hdcs.log.service;

import java.util.List;

import kr.kwater.hdcs.log.vo.LogVO;

public interface LogService {

    List<LogVO> getLogList(LogVO vo) throws Exception;

    int getLogCount(LogVO vo) throws Exception;
}
