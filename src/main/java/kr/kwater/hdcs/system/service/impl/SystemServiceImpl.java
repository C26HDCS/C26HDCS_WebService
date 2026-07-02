package kr.kwater.hdcs.system.service.impl;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.stereotype.Service;

import kr.kwater.hdcs.system.service.SystemService;
import kr.kwater.hdcs.system.vo.SystemVO;

@Service
public class SystemServiceImpl extends EgovAbstractServiceImpl implements SystemService {

    @Override
    public SystemVO getSystemInfo() throws Exception {
        SystemVO vo = new SystemVO();
        vo.setAppName("QOS Web Service");
        vo.setVersion("1.0.0");
        vo.setJavaVersion(System.getProperty("java.version"));
        vo.setOsName(System.getProperty("os.name"));
        vo.setDbType("H2 In-Memory");
        return vo;
    }
}
