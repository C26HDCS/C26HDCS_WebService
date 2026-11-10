package kr.kwater.hdcs.setting.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviceConfigVO {
    private Long   deviceId;
    private String equipId;
    private String equipType;
    private String obsCode;
    private String nmsDeviceName;
    private String displayName;
    private String groupName;
    private String nmsStatus;
    private String configStatus;
    private String lastReceivedAt;
    private String lastReceiveStatus;
    private String stationAddress;
    private String deviceStatus;

    public Long   getDeviceId()          { return deviceId; }
    public void   setDeviceId(Long v)    { this.deviceId = v; }
    public String getEquipId()           { return equipId; }
    public void   setEquipId(String v)   { this.equipId = v; }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getEquipDisplayId() {
        if (equipId == null || obsCode == null || obsCode.isBlank()) {
            return equipId;
        }
        String id = equipId.strip();
        String suffix = "[" + obsCode.strip() + "]";
        if (id.endsWith(suffix)) {
            String name = id.substring(0, id.length() - suffix.length()).stripTrailing();
            if (!name.isEmpty()) {
                return name;
            }
        }
        return equipId;
    }

    public String getEquipType()         { return equipType; }
    public void   setEquipType(String v) { this.equipType = v; }
    public String getObsCode()           { return obsCode; }
    public void   setObsCode(String v)   { this.obsCode = v; }
    public String getNmsDeviceName()         { return nmsDeviceName; }
    public void   setNmsDeviceName(String v) { this.nmsDeviceName = v; }
    public String getDisplayName()           { return displayName; }
    public void   setDisplayName(String v)   { this.displayName = v; }
    public String getGroupName()             { return groupName; }
    public void   setGroupName(String v)     { this.groupName = v; }
    public String getNmsStatus()             { return nmsStatus; }
    public void   setNmsStatus(String v)     { this.nmsStatus = v; }
    public String getConfigStatus()          { return configStatus; }
    public void   setConfigStatus(String v)  { this.configStatus = v; }
    public String getLastReceivedAt()        { return lastReceivedAt; }
    public void   setLastReceivedAt(String v){ this.lastReceivedAt = v; }
    public String getLastReceiveStatus()         { return lastReceiveStatus; }
    public void   setLastReceiveStatus(String v) { this.lastReceiveStatus = v; }
    public String getStationAddress() { return stationAddress; }
    public void   setStationAddress(String v) { this.stationAddress = v; }
    public String getDeviceStatus() { return deviceStatus; }
    public void   setDeviceStatus(String v) { this.deviceStatus = v; }
}
