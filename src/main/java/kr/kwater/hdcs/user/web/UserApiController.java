package kr.kwater.hdcs.user.web;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.kwater.hdcs.user.dao.UserDAO;
import kr.kwater.hdcs.user.dto.UserCreateDTO;
import kr.kwater.hdcs.user.dto.UserResponseDTO;

@RestController
@RequestMapping("/api/user")
public class UserApiController {

    private final UserDAO userDAO;
    private final PasswordEncoder passwordEncoder;

    public UserApiController(UserDAO userDAO, PasswordEncoder passwordEncoder) {
        this.userDAO = userDAO;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/list")
    public List<UserResponseDTO> getUserList() {
        return userDAO.findAllUsers();
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserCreateDTO dto) {
        if (userDAO.countByUserId(dto.getUserId()) > 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "이미 사용 중인 아이디입니다."));
        }
        dto.setPassword(passwordEncoder.encode(dto.getPassword()));
        userDAO.insertUser(dto);
        return ResponseEntity.ok(Map.of("message", "등록되었습니다."));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUser(@PathVariable String userId, @RequestBody UserCreateDTO dto) {
        dto.setUserId(userId);
        userDAO.updateUserInfo(dto);
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            userDAO.updateUserPassword(userId, passwordEncoder.encode(dto.getPassword()));
        }
        return ResponseEntity.ok(Map.of("message", "수정되었습니다."));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable String userId) {
        userDAO.deleteByUserId(userId);
        return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
    }
}
