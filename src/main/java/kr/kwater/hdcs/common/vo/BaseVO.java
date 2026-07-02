package kr.kwater.hdcs.common.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * eGovFramework 4.x에서 제거된 EgovDefaultVO를 대체하는 페이징/검색 기본 VO
 */
@Getter
@Setter
public abstract class BaseVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 현재 페이지 번호 */
    private int pageIndex = 1;

    /** 페이지당 레코드 수 */
    private int recordCountPerPage = 10;

    /** 페이지 크기 (페이징 네비 블럭 수) */
    private int pageSize = 10;

    /** 조회 시작 인덱스 (OFFSET) */
    private int firstIndex = 0;

    /** 조회 종료 인덱스 */
    private int lastIndex = 0;

    /** 검색 조건 구분 */
    private String searchCondition = "";

    /** 검색 키워드 */
    private String searchKeyword = "";

}
