package kr.kwater.hdcs.setting.service.impl;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import kr.kwater.hdcs.setting.dao.SettingDAO;
import kr.kwater.hdcs.setting.service.SettingService;
import kr.kwater.hdcs.setting.vo.DeviceConfigVO;

@Service
@RequiredArgsConstructor
public class SettingServiceImpl implements SettingService {

    private final SettingDAO settingDAO;

    @Override
    public List<DeviceConfigVO> getDeviceConfigList() throws Exception {
        return settingDAO.selectDeviceConfigList();
    }

    @Override
    public List<String> getGroupNames() throws Exception {
        return settingDAO.selectGroupNames();
    }
}
