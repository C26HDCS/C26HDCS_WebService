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
    public int saveChanges(List<Map<String, Object>> items) throws Exception {
        if (items == null || items.isEmpty()) return 0;
        int count = 0;
        for (Map<String, Object> item : items) {
            Object deviceIdObj = item.get("deviceId");
            if (deviceIdObj == null) continue;
            int deviceId = ((Number) deviceIdObj).intValue();

            String displayName = (String) item.get("displayName");
            String groupName   = (String) item.get("groupName");

            Map<String, Object> deviceInfo = settingDAO.selectDeviceInfoById(deviceId);
            if (deviceInfo == null) continue;

            Object stationId = deviceInfo.get("stationId");

            if (displayName != null) {
                if (stationId != null) {
                    Map<String, Object> update = new HashMap<>();
                    update.put("stationId",   stationId);
                    update.put("displayName", displayName);
                    settingDAO.updateStationName(update);
                } else if (!displayName.isEmpty()) {
                    String obsCode = (String) item.get("obsCode");
                    Map<String, Object> newStation = new HashMap<>();
                    newStation.put("obsCode", obsCode != null ? obsCode : "");
                    newStation.put("name",    displayName);
                    settingDAO.insertStation(newStation);

                    Map<String, Object> link = new HashMap<>();
                    link.put("deviceId",  deviceId);
                    link.put("stationId", newStation.get("stationId"));
                    settingDAO.updateDeviceStationIdById(link);
                }
            }

            if (groupName != null) {
                Map<String, Object> groupUpdate = new HashMap<>();
                groupUpdate.put("deviceId", deviceId);
                if (groupName.isEmpty()) {
                    groupUpdate.put("groupId", null);
                } else {
                    groupUpdate.put("groupId", settingDAO.selectGroupIdByName(groupName));
                }
                settingDAO.updateDeviceGroup(groupUpdate);
            }

            count++;
        }
        return count;
    }

    @Override
    public List<Map<String, Object>> getGroupList() throws Exception {
        return settingDAO.selectGroupList();
    }

    @Override
    public void addGroup(String groupName) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("groupName", groupName);
        settingDAO.insertGroup(data);
    }

    @Override
    public void updateGroup(long groupId, String groupName) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("groupId",   groupId);
        data.put("groupName", groupName);
        settingDAO.updateGroup(data);
    }

    @Override
    public void deleteGroup(long groupId) throws Exception {
        int count = settingDAO.countDevicesByGroup(groupId);
        if (count > 0) {
            throw new IllegalStateException("장비 " + count + "대가 배정된 그룹은 삭제할 수 없습니다. 먼저 장비의 그룹을 변경해주세요.");
        }
        settingDAO.deleteGroup(groupId);
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
