package kr.kwater.hdcs.log.vo;

import kr.kwater.hdcs.common.vo.BaseVO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogVO extends BaseVO {

    private Long   id;
    private String level;
    private String message;
    private String regDt;
}
