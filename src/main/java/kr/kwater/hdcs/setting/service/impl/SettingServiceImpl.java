package kr.kwater.hdcs.setting.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public int registerFromCsv(List<Map<String, String>> rows) throws Exception {
        if (rows == null || rows.isEmpty()) return 0;
        int count = 0;
        for (Map<String, String> row : rows) {
            String equipId     = row.get("equipId");
            String obsCode     = row.get("obsCode");
            String displayName = row.get("displayName");
            if (equipId == null || equipId.isEmpty()) continue;
            if (displayName == null || displayName.isEmpty()) continue;

            Map<String, Object> deviceInfo = settingDAO.selectDeviceForCsv(equipId);
            if (deviceInfo == null) continue;

            Object stationId = deviceInfo.get("stationId");
            if (stationId != null) {
                // 이미 관측소가 연결된 경우: 이름과 코드만 업데이트
                Map<String, Object> update = new HashMap<>();
                update.put("stationId", stationId);
                update.put("obsCode",   obsCode);
                update.put("name",      displayName);
                settingDAO.updateStationById(update);
            } else {
                // 관측소 미연결: 신규 생성 후 장비에 연결
                Map<String, Object> newStation = new HashMap<>();
                newStation.put("obsCode", obsCode);
                newStation.put("name",    displayName);
                settingDAO.insertStation(newStation);

                Map<String, Object> link = new HashMap<>();
                link.put("equipId",   equipId);
                link.put("stationId", newStation.get("stationId"));
                settingDAO.updateDeviceStationId(link);
            }
            count++;
        }
        return count;
    }
}
