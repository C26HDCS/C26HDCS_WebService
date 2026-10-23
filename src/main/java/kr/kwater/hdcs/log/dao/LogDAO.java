package kr.kwater.hdcs.log.dao;

import org.apache.ibatis.annotations.Mapper;

import kr.kwater.hdcs.log.vo.LogVO;

import java.util.List;

@Mapper
public interface LogDAO {

    List<LogVO> selectLogList(LogVO vo);

    int selectLogCount(LogVO vo);

    int insertLog(LogVO vo);

    int insertEvent(LogVO vo);

    int insertSystemEvent(LogVO vo);
}
