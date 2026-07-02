package kr.kwater.hdcs.system.service;

import kr.kwater.hdcs.system.vo.SystemVO;

public interface SystemService {

    SystemVO getSystemInfo() throws Exception;
}
