package kr.kwater.hdcs.quality.service;

import java.util.List;

import kr.kwater.hdcs.quality.vo.QualityVO;

public interface QualityService {

    List<QualityVO> getQualityList(QualityVO vo) throws Exception;

    int getQualityCount(QualityVO vo) throws Exception;
}
