package kr.kwater.hdcs.alarm.vo;

import kr.kwater.hdcs.common.vo.BaseVO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlarmVO extends BaseVO {

    private Long   id;
    private String type;
    private String target;
    private String severity;
    private String status;
    private String regDt;
}
