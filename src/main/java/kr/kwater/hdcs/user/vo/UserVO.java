package kr.kwater.hdcs.user.vo;

import java.time.LocalDateTime;

public class UserVO {
    private String userId;         // 사용자 ID (user_id)
    private String password;       // 비밀번호 (password)
    private String userName;       // 사용자명 (user_name)
    private String email;          // 이메일 (email)
    private String phoneNumber;    // 전화번호 (phone_number)
    private String useYn;          // 사용여부 (use_yn)
    private String roleType;       // 권한/역할 (role_type)
    private LocalDateTime createdAt; // 생성일시 (created_at)

    // Getter / Setter
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getUseYn() { return useYn; }
    public void setUseYn(String useYn) { this.useYn = useYn; }

    public String getRoleType() { return roleType; }
    public void setRoleType(String roleType) { this.roleType = roleType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}