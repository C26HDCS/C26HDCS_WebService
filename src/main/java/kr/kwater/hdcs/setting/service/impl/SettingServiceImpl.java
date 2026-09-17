package kr.kwater.hdcs.setting.service.impl;

import java.util.List;
import java.util.Map;

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

    @Override
    public int registerFromCsv(List<Map<String, String>> rows) throws Exception {
        if (rows == null || rows.isEmpty()) return 0;
        settingDAO.upsertObsStation(rows);
        return rows.size();
    }
}
