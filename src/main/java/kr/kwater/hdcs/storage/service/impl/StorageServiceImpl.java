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

@Service
@RequiredArgsConstructor
public class StorageServiceImpl extends EgovAbstractServiceImpl implements StorageService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final StorageDAO storageDAO;

    @Override
    public List<StorageDeviceVO> getDevices() {
        File[] roots = File.listRoots();
        if (roots == null) {
            return Collections.emptyList();
        }

        return Arrays.stream(roots)
                .filter(File::exists)
                .map(this::toDevice)
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

    private StorageDeviceVO toDevice(File root) {
        long totalBytes = safeSpace(root::getTotalSpace);
        long usableBytes = safeSpace(root::getUsableSpace);
        long usedBytes = totalBytes > 0 ? Math.max(totalBytes - usableBytes, 0L) : 0L;
        double usagePercent = totalBytes > 0
                ? Math.round((usedBytes * 1000.0) / totalBytes) / 10.0
                : 0.0;

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
        vo.setStatus(toStatus(usagePercent));
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

    private String toStatus(double usagePercent) {
        if (usagePercent >= 90.0) {
            return "danger";
        }
        if (usagePercent >= 80.0) {
            return "warning";
        }
        return "normal";
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
}
