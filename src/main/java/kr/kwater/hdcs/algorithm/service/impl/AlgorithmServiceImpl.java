package kr.kwater.hdcs.algorithm.service.impl;

import lombok.RequiredArgsConstructor;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.stereotype.Service;

import kr.kwater.hdcs.algorithm.dao.AlgorithmDAO;
import kr.kwater.hdcs.algorithm.service.AlgorithmService;
import kr.kwater.hdcs.algorithm.vo.AlgorithmVO;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlgorithmServiceImpl extends EgovAbstractServiceImpl implements AlgorithmService {

    private final AlgorithmDAO algorithmDAO;

    @Override
    public List<AlgorithmVO> getAlgorithmList(AlgorithmVO vo) throws Exception {
        return algorithmDAO.selectAlgorithmList(vo);
    }

    @Override
    public int updateEnabled(AlgorithmVO vo) throws Exception {
        return algorithmDAO.updateAlgorithmEnabled(vo);
    }
}
