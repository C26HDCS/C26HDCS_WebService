package kr.kwater.hdcs.dashboard.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ObservationStationVO {
    private Long   stationId;
    private String name;
    private Double lat;
    private Double lng;
    private String address;
    private String createdAt;
}
