package kr.kwater.hdcs.common.web;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.kwater.hdcs.user.dao.UserDAO;
import kr.kwater.hdcs.user.dto.UserCreateDTO;

@RestController
@RequestMapping("/api/public")
public class PublicApiController {

    private final UserDAO userDAO;
    private final PasswordEncoder passwordEncoder;

    public PublicApiController(UserDAO userDAO, PasswordEncoder passwordEncoder) {
        this.userDAO = userDAO;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/check-id")
    public ResponseEntity<?> checkId(@RequestParam String userId) {
        boolean taken = userDAO.countByUserId(userId) > 0;
        if (taken) {
            return ResponseEntity.ok(Map.of("available", false, "message", "이미 사용 중인 아이디입니다."));
        }
        return ResponseEntity.ok(Map.of("available", true, "message", "사용 가능한 아이디입니다."));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserCreateDTO dto) {
        if (dto.getUserId() == null || dto.getUserId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "아이디를 입력해 주세요."));
        }
        if (userDAO.countByUserId(dto.getUserId()) > 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "이미 사용 중인 아이디입니다."));
        }
        if (dto.getRole() == null || dto.getRole().isBlank()) {
            dto.setRole("USER");
        }
        dto.setPassword(passwordEncoder.encode(dto.getPassword()));
        userDAO.insertUser(dto);
        return ResponseEntity.ok(Map.of("message", "회원 가입이 완료되었습니다."));
    }
}
