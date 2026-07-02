package kr.kwater.hdcs.quality.vo;

import kr.kwater.hdcs.common.vo.BaseVO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QualityVO extends BaseVO {

    private Long   id;
    private String name;
    private String status;
    private int    progress;
    private String regDt;
}
