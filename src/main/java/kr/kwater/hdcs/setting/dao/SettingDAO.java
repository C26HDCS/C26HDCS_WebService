package kr.kwater.hdcs.setting.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.kwater.hdcs.setting.vo.DeviceConfigVO;

@Mapper
public interface SettingDAO {
    List<DeviceConfigVO> selectDeviceConfigList();
    List<String> selectGroupNames();
    void upsertObsStation(@Param("list") List<Map<String, String>> list);
}
