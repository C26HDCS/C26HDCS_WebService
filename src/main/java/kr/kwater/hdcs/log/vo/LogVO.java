package kr.kwater.hdcs.log.vo;

import kr.kwater.hdcs.common.vo.BaseVO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogVO extends BaseVO {

    private Long id;

    /** 구분: LOG(장치 로그) / EVENT(사용자 이벤트) */
    private String category;

    /** category가 LOG일 때만 값 존재 */
    private Long deviceId;

    /** category가 LOG일 때만 값 존재 */
    private String deviceName;

    /** category가 EVENT일 때만 값 존재 */
    private String userId;

    /** category가 LOG: 로그 레벨, category가 EVENT: 이벤트 유형 */
    private String level;

    private String message;
    private String regDt;

    /** 검색: 유형. 로그면 레벨(INFO/WARN/ERROR/DEBUG), 이벤트면 이벤트 유형(LOGIN_SUCCESS 등) */
    private String searchLevel;
    private String searchDateFrom;
    private String searchDateTo;
}
