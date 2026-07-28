package kr.kwater.hdcs.storage.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StorageDeleteTargetVO {

    private Long id;
    private String targetName;
    private String deleteMode;
    private String targetType;
    private String basePath;
    private String filePattern;
    private String fileExtension;
    private Integer minAgeDays;
    private Long minSizeBytes;
    private String minSizeText;
    private Boolean enabled;
    private String description;
    private String regDt;
}
