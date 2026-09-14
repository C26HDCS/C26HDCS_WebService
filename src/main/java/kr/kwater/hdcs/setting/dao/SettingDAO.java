package kr.kwater.hdcs.setting.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import kr.kwater.hdcs.setting.vo.DeviceConfigVO;

@Mapper
public interface SettingDAO {
    List<DeviceConfigVO> selectDeviceConfigList();
    List<String> selectGroupNames();
}
