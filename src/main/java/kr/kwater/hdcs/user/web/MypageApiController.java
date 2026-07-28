package kr.kwater.hdcs.user.web;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.kwater.hdcs.user.dao.UserDAO;
import kr.kwater.hdcs.user.dto.UserCreateDTO;
import kr.kwater.hdcs.user.vo.UserVO;

@RestController
@RequestMapping("/api/mypage")
public class MypageApiController {

    private final UserDAO userDAO;
    private final PasswordEncoder passwordEncoder;

    public MypageApiController(UserDAO userDAO, PasswordEncoder passwordEncoder) {
        this.userDAO = userDAO;
        this.passwordEncoder = passwordEncoder;
    }

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        UserVO user = userDAO.findByUserId(currentUserId());
        if (user == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of(
            "userId", user.getUserId(),
            "name",   user.getUserName()  != null ? user.getUserName()  : "",
            "email",  user.getEmail()     != null ? user.getEmail()     : ""
        ));
    }

    @PostMapping("/profile")
    public ResponseEntity<?> saveProfile(@RequestBody Map<String, String> body) {
        String userId = currentUserId();
        UserVO user = userDAO.findByUserId(userId);
        if (user == null) return ResponseEntity.notFound().build();

        String newEmail = (body.get("email") != null ? body.get("email") : "").trim();
        if (newEmail.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "이메일을 입력해 주세요."));
        }

        UserCreateDTO dto = new UserCreateDTO();
        dto.setUserId(userId);
        dto.setName(user.getUserName());
        dto.setRole(user.getRoleType() != null ? user.getRoleType().trim() : "USER");
        dto.setEmail(newEmail);
        userDAO.updateUserInfo(dto);
        return ResponseEntity.ok(Map.of("message", "저장되었습니다."));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body) {
        String userId = currentUserId();
        UserVO user = userDAO.findByUserId(userId);
        if (user == null) return ResponseEntity.notFound().build();

        String current = body.getOrDefault("current", "");
        String newPw   = body.getOrDefault("newPw",   "");
        String confirm = body.getOrDefault("confirm",  "");

        if (!passwordEncoder.matches(current, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "현재 비밀번호가 올바르지 않습니다."));
        }
        if (newPw.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "새 비밀번호를 입력해 주세요."));
        }
        if (!newPw.equals(confirm)) {
            return ResponseEntity.badRequest().body(Map.of("message", "새 비밀번호가 일치하지 않습니다."));
        }
        userDAO.updateUserPassword(userId, passwordEncoder.encode(newPw));
        return ResponseEntity.ok(Map.of("message", "비밀번호가 변경되었습니다."));
    }
}
