package kr.kwater.hdcs.algorithm.vo;

import kr.kwater.hdcs.common.vo.BaseVO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlgorithmVO extends BaseVO {

    private Long    id;
    private String  name;
    private String  type;
    private boolean enabled;
    private String  regDt;
}
