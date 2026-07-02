package kr.kwater.hdcs.dashboard.dao;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DashboardDAO {

    int selectActiveAlgorithmCount();

    int selectPendingAlarmCount();

    int selectTotalQualityCount();
}
