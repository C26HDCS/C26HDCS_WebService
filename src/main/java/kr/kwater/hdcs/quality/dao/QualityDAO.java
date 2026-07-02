package kr.kwater.hdcs.quality.dao;

import org.apache.ibatis.annotations.Mapper;

import kr.kwater.hdcs.quality.vo.QualityVO;

import java.util.List;

@Mapper
public interface QualityDAO {

    List<QualityVO> selectQualityList(QualityVO vo);

    int selectQualityCount(QualityVO vo);
}
