package kr.kwater.hdcs.dashboard.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import kr.kwater.hdcs.dashboard.vo.DeviceVO;

@Mapper
public interface DashboardDAO {

    int selectActiveAlgorithmCount();

    int selectPendingAlarmCount();

    int selectTotalQualityCount();

    int selectTotalDeviceCount();

    int selectNormalCount();

    int selectFaultCount();

    int selectWarnCount();

    List<DeviceVO> selectAllDevices();
}
