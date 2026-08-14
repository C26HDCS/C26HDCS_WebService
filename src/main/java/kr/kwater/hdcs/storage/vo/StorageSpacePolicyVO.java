package kr.kwater.hdcs.storage.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StorageSpacePolicyVO {

    private Long id;
    private String targetPath;
    private Double warningPercent;
    private Double dangerPercent;
    private Long minFreeBytes;
    private String minFreeText;
    private Boolean enabled;
    private String description;
    private String regDt;
    private String updDt;
    private long totalBytes;
    private long usedBytes;
    private long usableBytes;
    private double usagePercent;
    private String totalText;
    private String usedText;
    private String usableText;
    private String status;
    private String statusReason;
    private Boolean deletable;
}
