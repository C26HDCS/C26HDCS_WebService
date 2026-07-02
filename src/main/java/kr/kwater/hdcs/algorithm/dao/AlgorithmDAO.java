package kr.kwater.hdcs.algorithm.dao;

import org.apache.ibatis.annotations.Mapper;

import kr.kwater.hdcs.algorithm.vo.AlgorithmVO;

import java.util.List;

@Mapper
public interface AlgorithmDAO {

    List<AlgorithmVO> selectAlgorithmList(AlgorithmVO vo);

    int updateAlgorithmEnabled(AlgorithmVO vo);
}
