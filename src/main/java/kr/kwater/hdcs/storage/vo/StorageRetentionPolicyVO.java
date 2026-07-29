package kr.kwater.hdcs.storage.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StorageRetentionPolicyVO {

    private Long id;
    private String fileTypeName;
    private String fileExtension;
    private Integer retentionDays;
    private Boolean enabled;
    private String description;
    private String regDt;
    private String updDt;
}
