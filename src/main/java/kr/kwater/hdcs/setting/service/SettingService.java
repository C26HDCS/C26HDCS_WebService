package kr.kwater.hdcs.setting.service;

import java.util.List;
import java.util.Map;

import kr.kwater.hdcs.setting.vo.DeviceConfigVO;

public interface SettingService {
    List<DeviceConfigVO> getDeviceConfigList() throws Exception;
    List<String> getGroupNames() throws Exception;
    int registerFromCsv(List<Map<String, String>> rows) throws Exception;
}
