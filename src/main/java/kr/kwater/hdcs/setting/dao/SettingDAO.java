package kr.kwater.hdcs.setting.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import kr.kwater.hdcs.setting.vo.DeviceConfigVO;

@Mapper
public interface SettingDAO {
    List<DeviceConfigVO> selectDeviceConfigList();
    List<String> selectGroupNames();
    int updateObsStation(Map<String, String> row);
    void insertObsStation(Map<String, String> row);
}
