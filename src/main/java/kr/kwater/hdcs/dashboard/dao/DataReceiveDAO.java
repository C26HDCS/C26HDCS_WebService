package kr.kwater.hdcs.dashboard.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.kwater.hdcs.dashboard.vo.DeviceStatusHistoryVO;

@Mapper
public interface DataReceiveDAO {

    String selectDeviceType(@Param("deviceId") String deviceId);

    List<DeviceStatusHistoryVO> selectDcsData(@Param("deviceId") String deviceId);

    List<DeviceStatusHistoryVO> selectKaData(@Param("deviceId") String deviceId);
}
