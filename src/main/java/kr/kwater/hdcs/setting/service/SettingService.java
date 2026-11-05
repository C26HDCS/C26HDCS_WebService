package kr.kwater.hdcs.setting.service;

import java.util.List;
import java.util.Map;

import kr.kwater.hdcs.setting.vo.DeviceConfigVO;

public interface SettingService {
    List<DeviceConfigVO> getDeviceConfigList() throws Exception;
    List<String> getGroupNames() throws Exception;
    List<String> getEquipTypes() throws Exception;
    /** CSV 등록 전 중복 확인: 이미 관측소가 연결된 장비의 기존 값과 CSV 값을 비교해 돌려준다. DB는 바꾸지 않는다. */
    Map<String, Object> checkCsvDuplicates(List<Map<String, String>> rows) throws Exception;
    int registerFromCsv(List<Map<String, String>> rows) throws Exception;
    int saveChanges(List<Map<String, Object>> items) throws Exception;

    List<Map<String, Object>> getGroupList() throws Exception;
    void addGroup(String groupName) throws Exception;
    void updateGroup(long groupId, String groupName) throws Exception;
    void deleteGroup(long groupId) throws Exception;
}
