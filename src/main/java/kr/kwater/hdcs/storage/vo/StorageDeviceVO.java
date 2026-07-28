package kr.kwater.hdcs.storage.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StorageDeviceVO {

    private String name;
    private String path;
    private long totalBytes;
    private long usedBytes;
    private long usableBytes;
    private double usagePercent;
    private String totalText;
    private String usedText;
    private String usableText;
    private String status;
}
