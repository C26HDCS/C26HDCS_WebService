package kr.kwater.hdcs.quality.service.impl;

import lombok.RequiredArgsConstructor;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.stereotype.Service;

import kr.kwater.hdcs.quality.dao.QualityDAO;
import kr.kwater.hdcs.quality.service.QualityService;
import kr.kwater.hdcs.quality.vo.QualityVO;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QualityServiceImpl extends EgovAbstractServiceImpl implements QualityService {

    private final QualityDAO qualityDAO;

    @Override
    public List<QualityVO> getQualityList(QualityVO vo) throws Exception {
        return qualityDAO.selectQualityList(vo);
    }

    @Override
    public int getQualityCount(QualityVO vo) throws Exception {
        return qualityDAO.selectQualityCount(vo);
    }
}
