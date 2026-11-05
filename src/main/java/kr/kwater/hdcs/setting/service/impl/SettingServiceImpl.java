package kr.kwater.hdcs.setting.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public List<String> getEquipTypes() throws Exception {
        return settingDAO.selectEquipTypes();
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
            throw new IllegalStateException("장비 " + count + "대가 배정된 유역은 삭제할 수 없습니다. 먼저 장비의 유역을 변경해주세요.");
        }
        settingDAO.deleteGroup(groupId);
    }

    @Override
    public Map<String, Object> checkCsvDuplicates(List<Map<String, String>> rows) throws Exception {
        // 같은 장비ID가 여러 번 있으면 마지막 행이 등록되므로, 장비ID마다 마지막 행 기준으로 한 건으로 센다.
        Map<String, Map<String, Object>> found = new LinkedHashMap<>();   // 중복
        Set<String> newIds      = new HashSet<>();                        // 신규
        Set<String> notFoundIds = new HashSet<>();                        // 일치하는 장비 없음
        if (rows != null) {
            for (Map<String, String> row : rows) {
                String equipId     = row.get("equipId");
                String displayName = row.get("displayName");
                if (equipId == null || equipId.isEmpty()) continue;
                if (displayName == null || displayName.isEmpty()) continue;

                String groupName = row.get("groupName");   // null 이면 CSV 에 유역 컬럼이 없는 것 → 기존 유역을 건드리지 않는다.
                resolveGroupId(groupName);                  // 등록되지 않은 유역이면 여기서 막는다.

                found.remove(equipId);
                newIds.remove(equipId);
                notFoundIds.remove(equipId);

                Map<String, Object> info = settingDAO.selectDeviceStationForCsv(equipId);
                if (info == null) { notFoundIds.add(equipId); continue; }

                boolean hadStation = info.get("stationId") != null;
                String oldName  = nullToEmpty(info.get("oldName"));
                String oldCode  = nullToEmpty(info.get("oldCode"));
                String oldGroup = nullToEmpty(info.get("oldGroup"));
                String newCode  = nullToEmpty(row.get("obsCode"));
                String newGroup = nullToEmpty(groupName);
                boolean groupSpecified = groupName != null;
                boolean groupChanges   = groupSpecified && !oldGroup.equals(newGroup);

                // 중복: 관측소가 이미 연결된 장비. 관측소가 없어도 이미 있던 유역이 바뀌면 알려 준다.
                if (!hadStation && !(groupChanges && !oldGroup.isEmpty())) { newIds.add(equipId); continue; }

                Map<String, Object> item = new HashMap<>();
                item.put("equipId", equipId);
                item.put("hadStation", hadStation);
                item.put("oldName", oldName);
                item.put("oldCode", oldCode);
                item.put("newName", displayName);
                item.put("newCode", newCode);
                item.put("groupSpecified", groupSpecified);
                item.put("oldGroup", oldGroup);
                item.put("newGroup", newGroup);
                item.put("same", hadStation && oldName.equals(displayName) && oldCode.equals(newCode) && !groupChanges);
                found.put(equipId, item);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("duplicates", new ArrayList<>(found.values()));
        result.put("duplicateCount", found.size());
        result.put("newCount", newIds.size());
        result.put("notFoundCount", notFoundIds.size());
        result.put("total", found.size() + newIds.size() + notFoundIds.size());
        return result;
    }

    /** 유역(그룹)명 → group_id. null/빈 값이면 null(미설정). 등록되지 않은 이름이면 예외. */
    private Integer resolveGroupId(String groupName) throws Exception {
        if (groupName == null || groupName.isEmpty()) return null;
        Integer groupId = settingDAO.selectGroupIdByName(groupName);
        if (groupId == null) {
            throw new IllegalArgumentException("등록되지 않은 유역입니다: " + groupName);
        }
        return groupId;
    }

    private static String nullToEmpty(Object v) {
        return v == null ? "" : v.toString();
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

            // 유역: 컬럼이 없으면(null) 그대로 두고, 컬럼은 있는데 비어 있으면 미설정으로 바꾼다.
            String groupName = row.get("groupName");
            Integer groupId  = resolveGroupId(groupName);

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
            if (groupName != null) {
                Map<String, Object> groupUpdate = new HashMap<>();
                groupUpdate.put("deviceId", deviceInfo.get("deviceId"));
                groupUpdate.put("groupId",  groupId);
                settingDAO.updateDeviceGroup(groupUpdate);
            }
            count++;
        }
        return count;
    }
}
