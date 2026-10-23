package kr.kwater.hdcs.dashboard.vo;

public record PipelineComponentVO(String dataKind, String component, String scope, String status,
                                  String detail, String checkedAt, boolean expired) { }
