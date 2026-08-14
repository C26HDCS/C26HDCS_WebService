package kr.kwater.hdcs.storage.service.impl;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import kr.kwater.hdcs.storage.dao.StorageDAO;
import kr.kwater.hdcs.storage.service.StorageService;
import kr.kwater.hdcs.storage.vo.StorageDeleteResultVO;
import kr.kwater.hdcs.storage.vo.StorageDeleteTargetVO;
import kr.kwater.hdcs.storage.vo.StorageDeviceVO;
import kr.kwater.hdcs.storage.vo.StorageFileVO;
import kr.kwater.hdcs.storage.vo.StorageRetentionPolicyVO;
import kr.kwater.hdcs.storage.vo.StorageSpacePolicyVO;

@Service
@RequiredArgsConstructor
public class StorageServiceImpl extends EgovAbstractServiceImpl implements StorageService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final double DEFAULT_WARNING_PERCENT = 80.0;
    private static final double DEFAULT_DANGER_PERCENT = 90.0;

    private final StorageDAO storageDAO;

    @Override
    public List<StorageDeviceVO> getDevices() {
        File[] roots = File.listRoots();
        if (roots == null) {
            return Collections.emptyList();
        }

        List<StorageSpacePolicyVO> spacePolicies = selectSpacePoliciesSafely();
        return Arrays.stream(roots)
                .filter(File::exists)
                .map(root -> toDevice(root, spacePolicies))
                .sorted(Comparator.comparing(StorageDeviceVO::getPath))
                .collect(Collectors.toList());
    }

    @Override
    public List<StorageFileVO> getFiles(String path) {
        if (!StringUtils.hasText(path)) {
            return Collections.emptyList();
        }

        File directory = new File(path);
        if (!directory.exists() || !directory.isDirectory()) {
            throw new IllegalArgumentException("조회할 수 없는 폴더입니다.");
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }

        return Arrays.stream(files)
                .sorted(Comparator.comparing(File::isFile).thenComparing(File::getName, String.CASE_INSENSITIVE_ORDER))
                .limit(500)
                .map(this::toFile)
                .collect(Collectors.toList());
    }

    @Override
    public StorageDeleteResultVO deleteFiles(String directoryPath, List<String> filePaths) {
        if (!StringUtils.hasText(directoryPath)) {
            throw new IllegalArgumentException("삭제 기준 폴더가 없습니다.");
        }
        if (filePaths == null || filePaths.isEmpty()) {
            throw new IllegalArgumentException("삭제할 파일을 선택해주세요.");
        }

        File directory = toCanonicalFile(new File(directoryPath));
        if (!directory.exists() || !directory.isDirectory()) {
            throw new IllegalArgumentException("조회할 수 없는 폴더입니다.");
        }

        Set<String> uniquePaths = filePaths.stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        StorageDeleteResultVO result = new StorageDeleteResultVO();
        result.setRequestedCount(uniquePaths.size());

        for (String filePath : uniquePaths) {
            deleteSingleFile(directory, filePath, result);
        }

        result.setDeletedCount(result.getDeletedPaths().size());
        result.setSkippedCount(result.getSkippedPaths().size());
        result.setMessage(result.getDeletedCount() + "개 파일을 삭제했습니다.");
        return result;
    }

    @Override
    public List<StorageDeleteTargetVO> getDeleteTargets() {
        try {
            return storageDAO.selectDeleteTargetList().stream()
                    .peek(this::setDisplayValues)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            return Collections.emptyList();
        }
    }

    @Override
    public StorageDeleteTargetVO createDeleteTarget(StorageDeleteTargetVO vo) {
        if (vo == null) {
            throw new IllegalArgumentException("등록할 삭제 대상 기준이 없습니다.");
        }

        prepareDeleteTarget(vo);

        storageDAO.insertDeleteTarget(vo);
        Long createdId = vo.getId();
        if (createdId == null) {
            return setDisplayValues(vo);
        }

        return storageDAO.selectDeleteTargetList().stream()
                .filter(target -> createdId.equals(target.getId()))
                .findFirst()
                .map(this::setDisplayValues)
                .orElseGet(() -> setDisplayValues(vo));
    }

    @Override
    public StorageDeleteTargetVO updateDeleteTarget(Long id, StorageDeleteTargetVO vo) {
        if (id == null) {
            throw new IllegalArgumentException("삭제 대상 기준 ID가 없습니다.");
        }
        if (vo == null) {
            throw new IllegalArgumentException("수정할 삭제 대상 기준이 없습니다.");
        }

        vo.setId(id);
        prepareDeleteTarget(vo);

        int updatedCount = storageDAO.updateDeleteTarget(vo);
        if (updatedCount == 0) {
            throw new IllegalArgumentException("수정할 삭제 대상 기준이 없습니다.");
        }

        return storageDAO.selectDeleteTargetList().stream()
                .filter(target -> id.equals(target.getId()))
                .findFirst()
                .map(this::setDisplayValues)
                .orElse(vo);
    }

    @Override
    public void deleteDeleteTarget(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("삭제 대상 기준 ID가 없습니다.");
        }

        int deletedCount = storageDAO.deleteDeleteTarget(id);
        if (deletedCount == 0) {
            throw new IllegalArgumentException("삭제할 삭제 대상 기준이 없습니다.");
        }
    }

    @Override
    public List<StorageRetentionPolicyVO> getRetentionPolicies() {
        try {
            return storageDAO.selectRetentionPolicyList();
        } catch (DataAccessException ex) {
            return Collections.emptyList();
        }
    }

    @Override
    public StorageRetentionPolicyVO createRetentionPolicy(StorageRetentionPolicyVO vo) {
        if (vo == null) {
            throw new IllegalArgumentException("등록할 보관 기간 정책이 없습니다.");
        }

        prepareRetentionPolicy(vo, true);

        try {
            storageDAO.insertRetentionPolicy(vo);
        } catch (DataAccessException ex) {
            throw new IllegalArgumentException("이미 등록된 확장자입니다.", ex);
        }

        Long createdId = vo.getId();
        if (createdId == null) {
            return vo;
        }

        return storageDAO.selectRetentionPolicyList().stream()
                .filter(policy -> createdId.equals(policy.getId()))
                .findFirst()
                .orElse(vo);
    }

    @Override
    public StorageRetentionPolicyVO updateRetentionPolicy(Long id, StorageRetentionPolicyVO vo) {
        if (id == null) {
            throw new IllegalArgumentException("보관 기간 정책 ID가 없습니다.");
        }
        if (vo == null) {
            throw new IllegalArgumentException("수정할 보관 기간 정책이 없습니다.");
        }

        vo.setId(id);
        prepareRetentionPolicy(vo, false);

        int updatedCount = storageDAO.updateRetentionPolicy(vo);
        if (updatedCount == 0) {
            throw new IllegalArgumentException("수정할 보관 기간 정책이 없습니다.");
        }

        return storageDAO.selectRetentionPolicyList().stream()
                .filter(policy -> id.equals(policy.getId()))
                .findFirst()
                .orElse(vo);
    }

    @Override
    public void deleteRetentionPolicy(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("보관 기간 정책 ID가 없습니다.");
        }

        int deletedCount = storageDAO.deleteRetentionPolicy(id);
        if (deletedCount == 0) {
            throw new IllegalArgumentException("삭제할 보관 기간 정책이 없습니다.");
        }
    }

    @Override
    public List<StorageSpacePolicyVO> getSpacePolicies() {
        try {
            List<StorageSpacePolicyVO> policies = storageDAO.selectSpacePolicyList();
            if (policies.isEmpty()) {
                StorageSpacePolicyVO defaultPolicy = defaultSpacePolicy();
                storageDAO.insertSpacePolicy(defaultPolicy);
                policies = storageDAO.selectSpacePolicyList();
            }
            return policies.stream()
                    .peek(this::setDisplayValues)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            return Collections.singletonList(setDisplayValues(defaultSpacePolicy()));
        }
    }

    @Override
    public StorageSpacePolicyVO createSpacePolicy(StorageSpacePolicyVO vo) {
        if (vo == null) {
            throw new IllegalArgumentException("등록할 가용 공간 기준이 없습니다.");
        }

        prepareSpacePolicy(vo, true);

        try {
            storageDAO.insertSpacePolicy(vo);
        } catch (DataAccessException ex) {
            throw new IllegalArgumentException("이미 등록된 적용 경로입니다.", ex);
        }

        Long createdId = vo.getId();
        if (createdId == null) {
            return setDisplayValues(vo);
        }

        return storageDAO.selectSpacePolicyList().stream()
                .filter(policy -> createdId.equals(policy.getId()))
                .findFirst()
                .map(this::setDisplayValues)
                .orElseGet(() -> setDisplayValues(vo));
    }

    @Override
    public StorageSpacePolicyVO updateSpacePolicy(Long id, StorageSpacePolicyVO vo) {
        if (id == null) {
            throw new IllegalArgumentException("가용 공간 기준 ID가 없습니다.");
        }
        if (vo == null) {
            throw new IllegalArgumentException("수정할 가용 공간 기준이 없습니다.");
        }

        vo.setId(id);
        prepareSpacePolicy(vo, false);

        int updatedCount = storageDAO.updateSpacePolicy(vo);
        if (updatedCount == 0) {
            throw new IllegalArgumentException("수정할 가용 공간 기준이 없습니다.");
        }

        return storageDAO.selectSpacePolicyList().stream()
                .filter(policy -> id.equals(policy.getId()))
                .findFirst()
                .map(this::setDisplayValues)
                .orElseGet(() -> setDisplayValues(vo));
    }

    @Override
    public void deleteSpacePolicy(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("가용 공간 기준 ID가 없습니다.");
        }

        StorageSpacePolicyVO target = storageDAO.selectSpacePolicyList().stream()
                .filter(policy -> id.equals(policy.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("삭제할 가용 공간 기준이 없습니다."));

        if (isDefaultSpacePolicy(target)) {
            throw new IllegalArgumentException("전체 저장장치 공통 기준은 삭제할 수 없습니다.");
        }

        int deletedCount = storageDAO.deleteSpacePolicy(id);
        if (deletedCount == 0) {
            throw new IllegalArgumentException("삭제할 가용 공간 기준이 없습니다.");
        }
    }

    private void prepareDeleteTarget(StorageDeleteTargetVO vo) {
        if (!StringUtils.hasText(vo.getTargetName())) {
            throw new IllegalArgumentException("기준명을 입력해주세요.");
        }
        if (!StringUtils.hasText(vo.getBasePath())) {
            throw new IllegalArgumentException("기준 경로를 입력해주세요.");
        }

        vo.setTargetName(vo.getTargetName().trim());
        vo.setBasePath(vo.getBasePath().trim());
        vo.setDeleteMode(normalizeCode(vo.getDeleteMode(), "MANUAL"));
        vo.setTargetType(normalizeCode(vo.getTargetType(), "RULE"));
        vo.setFilePattern(trimToNull(vo.getFilePattern()));
        vo.setFileExtension(normalizeExtension(vo.getFileExtension()));
        vo.setDescription(trimToNull(vo.getDescription()));

        if (!"AUTO".equals(vo.getDeleteMode()) && !"MANUAL".equals(vo.getDeleteMode())) {
            throw new IllegalArgumentException("삭제 방식은 AUTO 또는 MANUAL만 가능합니다.");
        }
        if (!"FILE".equals(vo.getTargetType()) && !"RULE".equals(vo.getTargetType())) {
            throw new IllegalArgumentException("대상 유형은 FILE 또는 RULE만 가능합니다.");
        }
        if (vo.getMinAgeDays() != null && vo.getMinAgeDays() < 0) {
            throw new IllegalArgumentException("경과일 조건은 0 이상으로 입력해주세요.");
        }
        if (vo.getMinSizeBytes() != null && vo.getMinSizeBytes() < 0) {
            throw new IllegalArgumentException("최소 크기 조건은 0 이상으로 입력해주세요.");
        }
        if (vo.getEnabled() == null) {
            vo.setEnabled(Boolean.TRUE);
        }
    }

    private void prepareRetentionPolicy(StorageRetentionPolicyVO vo, boolean validateTypeFields) {
        if (vo == null || vo.getRetentionDays() == null) {
            throw new IllegalArgumentException("보관 기간을 입력해주세요.");
        }
        if (vo.getRetentionDays() < 1 || vo.getRetentionDays() > 3650) {
            throw new IllegalArgumentException("보관 기간은 1일부터 3650일 사이로 입력해주세요.");
        }

        if (validateTypeFields) {
            if (!StringUtils.hasText(vo.getFileTypeName())) {
                throw new IllegalArgumentException("파일 종류명을 입력해주세요.");
            }
            if (!StringUtils.hasText(vo.getFileExtension())) {
                throw new IllegalArgumentException("확장자를 입력해주세요.");
            }
            vo.setFileTypeName(vo.getFileTypeName().trim());
            vo.setFileExtension(normalizeExtension(vo.getFileExtension()));
            if (!StringUtils.hasText(vo.getFileExtension())) {
                throw new IllegalArgumentException("확장자를 입력해주세요.");
            }
            vo.setDescription(trimToNull(vo.getDescription()));
        }

        if (vo.getEnabled() == null) {
            vo.setEnabled(Boolean.TRUE);
        }
    }

    private void prepareSpacePolicy(StorageSpacePolicyVO vo, boolean validateTargetPath) {
        if (validateTargetPath && !StringUtils.hasText(vo.getTargetPath())) {
            throw new IllegalArgumentException("적용 경로를 입력해주세요.");
        }
        if (validateTargetPath) {
            vo.setTargetPath(normalizeSpaceTargetPath(vo.getTargetPath()));
            if (isDefaultSpacePolicy(vo)) {
                throw new IllegalArgumentException("전체 저장장치 공통 기준은 이미 기본 기준으로 관리됩니다.");
            }
        }
        if (vo.getWarningPercent() == null || vo.getDangerPercent() == null) {
            throw new IllegalArgumentException("주의/위험 기준을 입력해주세요.");
        }
        if (vo.getWarningPercent() < 0 || vo.getWarningPercent() > 100
                || vo.getDangerPercent() < 0 || vo.getDangerPercent() > 100) {
            throw new IllegalArgumentException("가용 공간 기준은 0부터 100 사이로 입력해주세요.");
        }
        if (vo.getWarningPercent() >= vo.getDangerPercent()) {
            throw new IllegalArgumentException("주의 기준은 위험 기준보다 작아야 합니다.");
        }
        if (vo.getMinFreeBytes() != null && vo.getMinFreeBytes() < 0) {
            throw new IllegalArgumentException("최소 여유 공간은 0 이상으로 입력해주세요.");
        }

        vo.setDescription(trimToNull(vo.getDescription()));
        if (vo.getEnabled() == null) {
            vo.setEnabled(Boolean.TRUE);
        }
    }

    private String normalizeSpaceTargetPath(String targetPath) {
        if (!StringUtils.hasText(targetPath)) {
            return null;
        }

        String trimmedPath = targetPath.trim();
        if ("ALL".equalsIgnoreCase(trimmedPath)) {
            return "ALL";
        }

        File targetDirectory = toCanonicalFile(new File(trimmedPath));
        if (!targetDirectory.exists() || !targetDirectory.isDirectory()) {
            throw new IllegalArgumentException("조회할 수 있는 폴더 경로를 입력해주세요.");
        }
        return targetDirectory.getAbsolutePath();
    }

    private List<StorageSpacePolicyVO> selectSpacePoliciesSafely() {
        try {
            List<StorageSpacePolicyVO> policies = storageDAO.selectSpacePolicyList();
            return policies.isEmpty() ? Collections.singletonList(defaultSpacePolicy()) : policies;
        } catch (DataAccessException ex) {
            return Collections.singletonList(defaultSpacePolicy());
        }
    }

    private StorageSpacePolicyVO findPolicy(String rootPath, List<StorageSpacePolicyVO> policies) {
        if (policies != null) {
            for (StorageSpacePolicyVO policy : policies) {
                if (StringUtils.hasText(policy.getTargetPath())
                        && policy.getTargetPath().equalsIgnoreCase(rootPath)) {
                    return normalizeSpacePolicy(policy);
                }
            }
            for (StorageSpacePolicyVO policy : policies) {
                if ("ALL".equalsIgnoreCase(policy.getTargetPath())) {
                    return normalizeSpacePolicy(policy);
                }
            }
        }
        return defaultSpacePolicy();
    }

    private StorageSpacePolicyVO defaultSpacePolicy() {
        StorageSpacePolicyVO policy = new StorageSpacePolicyVO();
        policy.setTargetPath("ALL");
        policy.setWarningPercent(DEFAULT_WARNING_PERCENT);
        policy.setDangerPercent(DEFAULT_DANGER_PERCENT);
        policy.setEnabled(Boolean.TRUE);
        policy.setDescription("전체 저장장치 공통 가용 공간 기준");
        return policy;
    }

    private StorageSpacePolicyVO normalizeSpacePolicy(StorageSpacePolicyVO policy) {
        if (policy.getWarningPercent() == null) {
            policy.setWarningPercent(DEFAULT_WARNING_PERCENT);
        }
        if (policy.getDangerPercent() == null) {
            policy.setDangerPercent(DEFAULT_DANGER_PERCENT);
        }
        if (policy.getEnabled() == null) {
            policy.setEnabled(Boolean.TRUE);
        }
        return policy;
    }

    private boolean isDefaultSpacePolicy(StorageSpacePolicyVO policy) {
        return policy != null && "ALL".equalsIgnoreCase(policy.getTargetPath());
    }

    private String normalizeCode(String value, String defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeExtension(String value) {
        String extension = trimToNull(value);
        if (extension == null) {
            return null;
        }
        while (extension.startsWith(".")) {
            extension = extension.substring(1);
        }
        return StringUtils.hasText(extension) ? extension.toLowerCase(Locale.ROOT) : null;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void deleteSingleFile(File directory, String filePath, StorageDeleteResultVO result) {
        File target = toCanonicalFile(new File(filePath));

        if (!target.exists() || !target.isFile()) {
            result.getSkippedPaths().add(filePath);
            return;
        }

        File parent = toCanonicalFile(target.getParentFile());
        if (!directory.equals(parent)) {
            result.getSkippedPaths().add(filePath);
            return;
        }

        try {
            java.nio.file.Files.delete(target.toPath());
            result.getDeletedPaths().add(target.getAbsolutePath());
        } catch (IOException | SecurityException ex) {
            result.getSkippedPaths().add(filePath);
        }
    }

    private File toCanonicalFile(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException ex) {
            throw new IllegalArgumentException("파일 경로를 확인할 수 없습니다.", ex);
        }
    }

    private StorageDeviceVO toDevice(File root, List<StorageSpacePolicyVO> policies) {
        long totalBytes = safeSpace(root::getTotalSpace);
        long usableBytes = safeSpace(root::getUsableSpace);
        long usedBytes = totalBytes > 0 ? Math.max(totalBytes - usableBytes, 0L) : 0L;
        double usagePercent = totalBytes > 0
                ? Math.round((usedBytes * 1000.0) / totalBytes) / 10.0
                : 0.0;
        StorageSpacePolicyVO policy = findPolicy(root.getAbsolutePath(), policies);

        StorageDeviceVO vo = new StorageDeviceVO();
        vo.setName(toDeviceName(root));
        vo.setPath(root.getAbsolutePath());
        vo.setTotalBytes(totalBytes);
        vo.setUsedBytes(usedBytes);
        vo.setUsableBytes(usableBytes);
        vo.setUsagePercent(usagePercent);
        vo.setTotalText(formatBytes(totalBytes));
        vo.setUsedText(formatBytes(usedBytes));
        vo.setUsableText(formatBytes(usableBytes));
        vo.setStatus(toStatus(usagePercent, usableBytes, policy));
        vo.setStatusReason(toStatusReason(usagePercent, usableBytes, policy, vo.getStatus()));
        vo.setPolicyWarningPercent(policy.getWarningPercent());
        vo.setPolicyDangerPercent(policy.getDangerPercent());
        vo.setPolicyMinFreeText(formatNullableBytes(policy.getMinFreeBytes()));
        return vo;
    }

    private StorageFileVO toFile(File file) {
        boolean directory = file.isDirectory();

        StorageFileVO vo = new StorageFileVO();
        vo.setName(file.getName());
        vo.setType(directory ? "폴더" : getExtension(file.getName()));
        vo.setPath(file.getAbsolutePath());
        vo.setDirectory(directory);
        vo.setSizeBytes(directory ? 0L : safeSpace(file::length));
        vo.setSizeText(directory ? "-" : formatBytes(vo.getSizeBytes()));
        vo.setModifiedAt(formatModifiedAt(file.lastModified()));
        return vo;
    }

    private long safeSpace(LongSupplier supplier) {
        try {
            return Math.max(supplier.getAsLong(), 0L);
        } catch (SecurityException ex) {
            return 0L;
        }
    }

    private String toDeviceName(File root) {
        String path = root.getAbsolutePath();
        if (path.length() >= 2 && path.charAt(1) == ':') {
            return "저장장치 " + path.substring(0, 2).toUpperCase(Locale.ROOT);
        }
        return path;
    }

    private String toStatus(double usagePercent, long usableBytes, StorageSpacePolicyVO policy) {
        if (!Boolean.TRUE.equals(policy.getEnabled())) {
            return "normal";
        }
        if (policy.getMinFreeBytes() != null && usableBytes <= policy.getMinFreeBytes()) {
            return "danger";
        }
        if (usagePercent >= policy.getDangerPercent()) {
            return "danger";
        }
        if (usagePercent >= policy.getWarningPercent()) {
            return "warning";
        }
        return "normal";
    }

    private String toStatusReason(double usagePercent, long usableBytes, StorageSpacePolicyVO policy, String status) {
        if (!Boolean.TRUE.equals(policy.getEnabled())) {
            return "가용 공간 기준 미사용";
        }
        if ("danger".equals(status)
                && policy.getMinFreeBytes() != null
                && usableBytes <= policy.getMinFreeBytes()) {
            return "여유 공간 " + formatBytes(usableBytes) + " / 최소 기준 " + formatBytes(policy.getMinFreeBytes());
        }
        if ("danger".equals(status)) {
            return "사용률 " + usagePercent + "% / 위험 기준 " + policy.getDangerPercent() + "%";
        }
        if ("warning".equals(status)) {
            return "사용률 " + usagePercent + "% / 주의 기준 " + policy.getWarningPercent() + "%";
        }
        return "가용 공간 기준 이내";
    }

    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "파일";
        }
        return fileName.substring(dotIndex + 1).toUpperCase(Locale.ROOT);
    }

    private String formatModifiedAt(long lastModified) {
        if (lastModified <= 0) {
            return "-";
        }
        return Instant.ofEpochMilli(lastModified)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(DATE_FORMAT);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }

        String[] units = {"KB", "MB", "GB", "TB", "PB"};
        double value = bytes;
        int unitIndex = -1;
        do {
            value = value / 1024.0;
            unitIndex++;
        } while (value >= 1024.0 && unitIndex < units.length - 1);

        return new DecimalFormat("0.#").format(value) + " " + units[unitIndex];
    }

    private String formatNullableBytes(Long bytes) {
        if (bytes == null) {
            return "-";
        }
        return formatBytes(bytes);
    }

    private StorageDeleteTargetVO setDisplayValues(StorageDeleteTargetVO target) {
        target.setMinSizeText(formatNullableBytes(target.getMinSizeBytes()));
        return target;
    }

    private StorageSpacePolicyVO setDisplayValues(StorageSpacePolicyVO policy) {
        normalizeSpacePolicy(policy);
        policy.setMinFreeText(formatNullableBytes(policy.getMinFreeBytes()));
        policy.setDeletable(!isDefaultSpacePolicy(policy));

        if (isDefaultSpacePolicy(policy)) {
            policy.setStatus("normal");
            policy.setStatusReason("저장장치 카드에 공통 적용");
            policy.setTotalText("-");
            policy.setUsedText("-");
            policy.setUsableText("-");
            return policy;
        }

        File targetDirectory = new File(policy.getTargetPath());
        if (!targetDirectory.exists() || !targetDirectory.isDirectory()) {
            policy.setStatus("danger");
            policy.setStatusReason("조회할 수 없는 경로");
            policy.setTotalText("-");
            policy.setUsedText("-");
            policy.setUsableText("-");
            return policy;
        }

        long totalBytes = safeSpace(targetDirectory::getTotalSpace);
        long usableBytes = safeSpace(targetDirectory::getUsableSpace);
        long usedBytes = totalBytes > 0 ? Math.max(totalBytes - usableBytes, 0L) : 0L;
        double usagePercent = totalBytes > 0
                ? Math.round((usedBytes * 1000.0) / totalBytes) / 10.0
                : 0.0;

        policy.setTotalBytes(totalBytes);
        policy.setUsedBytes(usedBytes);
        policy.setUsableBytes(usableBytes);
        policy.setUsagePercent(usagePercent);
        policy.setTotalText(formatBytes(totalBytes));
        policy.setUsedText(formatBytes(usedBytes));
        policy.setUsableText(formatBytes(usableBytes));
        policy.setStatus(toStatus(usagePercent, usableBytes, policy));
        policy.setStatusReason(toStatusReason(usagePercent, usableBytes, policy, policy.getStatus()));
        return policy;
    }
}
