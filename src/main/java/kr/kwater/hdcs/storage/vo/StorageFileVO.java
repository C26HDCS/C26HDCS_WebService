package kr.kwater.hdcs.storage.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StorageFileVO {

    private String name;
    private String type;
    private String path;
    private boolean directory;
    private long sizeBytes;
    private String sizeText;
    private String modifiedAt;
}
