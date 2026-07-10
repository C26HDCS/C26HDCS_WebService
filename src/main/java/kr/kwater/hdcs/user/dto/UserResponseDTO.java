package kr.kwater.hdcs.user.dto;

public class UserResponseDTO {
    private String userId;
    private String name;
    private String email;
    private String role;
    private boolean active;
    private String createdAt;

    public String getUserId()  { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getName()    { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail()   { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole()    { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isActive()  { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
