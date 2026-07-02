package kr.kwater.hdcs.system.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SystemVO {

    private String appName;
    private String version;
    private String javaVersion;
    private String osName;
    private String dbType;
}
