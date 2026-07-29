package kr.kwater.hdcs.storage.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.kwater.hdcs.storage.vo.StorageDeleteTargetVO;
import kr.kwater.hdcs.storage.vo.StorageRetentionPolicyVO;

@Mapper
public interface StorageDAO {

    List<StorageDeleteTargetVO> selectDeleteTargetList();

    int insertDeleteTarget(StorageDeleteTargetVO vo);

    int updateDeleteTarget(StorageDeleteTargetVO vo);

    int deleteDeleteTarget(@Param("id") Long id);

    List<StorageRetentionPolicyVO> selectRetentionPolicyList();

    int insertRetentionPolicy(StorageRetentionPolicyVO vo);

    int updateRetentionPolicy(StorageRetentionPolicyVO vo);

    int deleteRetentionPolicy(@Param("id") Long id);
}
