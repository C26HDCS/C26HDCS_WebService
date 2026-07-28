package kr.kwater.hdcs.storage.vo;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StorageDeleteRequestVO {

    private String directoryPath;
    private List<String> filePaths;
}
