package kr.kwater.hdcs.storage.vo;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StorageDeleteResultVO {

    private int requestedCount;
    private int deletedCount;
    private int skippedCount;
    private String message;
    private List<String> deletedPaths = new ArrayList<>();
    private List<String> skippedPaths = new ArrayList<>();
}
