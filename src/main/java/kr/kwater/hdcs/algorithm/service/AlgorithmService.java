package kr.kwater.hdcs.algorithm.service;

import java.util.List;

import kr.kwater.hdcs.algorithm.vo.AlgorithmVO;

public interface AlgorithmService {

    List<AlgorithmVO> getAlgorithmList(AlgorithmVO vo) throws Exception;

    int updateEnabled(AlgorithmVO vo) throws Exception;
}
