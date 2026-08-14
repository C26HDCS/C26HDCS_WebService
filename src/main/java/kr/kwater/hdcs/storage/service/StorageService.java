package kr.kwater.hdcs.storage.service;

import java.util.List;

import kr.kwater.hdcs.storage.vo.StorageDeviceVO;
import kr.kwater.hdcs.storage.vo.StorageDeleteResultVO;
import kr.kwater.hdcs.storage.vo.StorageDeleteTargetVO;
import kr.kwater.hdcs.storage.vo.StorageFileVO;
import kr.kwater.hdcs.storage.vo.StorageRetentionPolicyVO;
import kr.kwater.hdcs.storage.vo.StorageSpacePolicyVO;

public interface StorageService {

    List<StorageDeviceVO> getDevices();

    List<StorageFileVO> getFiles(String path);

    StorageDeleteResultVO deleteFiles(String directoryPath, List<String> filePaths);

    List<StorageDeleteTargetVO> getDeleteTargets();

    StorageDeleteTargetVO createDeleteTarget(StorageDeleteTargetVO vo);

    StorageDeleteTargetVO updateDeleteTarget(Long id, StorageDeleteTargetVO vo);

    void deleteDeleteTarget(Long id);

    List<StorageRetentionPolicyVO> getRetentionPolicies();

    StorageRetentionPolicyVO createRetentionPolicy(StorageRetentionPolicyVO vo);

    StorageRetentionPolicyVO updateRetentionPolicy(Long id, StorageRetentionPolicyVO vo);

    void deleteRetentionPolicy(Long id);

    List<StorageSpacePolicyVO> getSpacePolicies();

    StorageSpacePolicyVO createSpacePolicy(StorageSpacePolicyVO vo);

    StorageSpacePolicyVO updateSpacePolicy(Long id, StorageSpacePolicyVO vo);

    void deleteSpacePolicy(Long id);
}
