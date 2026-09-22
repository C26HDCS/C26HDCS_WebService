package kr.kwater.hdcs.setting.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import kr.kwater.hdcs.setting.vo.DeviceConfigVO;

@Mapper
public interface SettingDAO {
    List<DeviceConfigVO> selectDeviceConfigList();
    List<String> selectGroupNames();
    Map<String, Object> selectDeviceForCsv(String equipId);
    void updateStationById(Map<String, Object> data);
    void insertStation(Map<String, Object> data);
    void updateDeviceStationId(Map<String, Object> data);

    Map<String, Object> selectDeviceInfoById(int deviceId);
    void updateStationName(Map<String, Object> data);
    Integer selectGroupIdByName(String groupName);
    void updateDeviceGroup(Map<String, Object> data);
    void updateDeviceStationIdById(Map<String, Object> data);

    List<Map<String, Object>> selectGroupList();
    void insertGroup(Map<String, Object> data);
    void updateGroup(Map<String, Object> data);
    int countDevicesByGroup(long groupId);
    void deleteGroup(long groupId);
}
