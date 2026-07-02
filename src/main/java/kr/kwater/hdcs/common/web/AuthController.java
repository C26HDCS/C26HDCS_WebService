package kr.kwater.hdcs.common.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import kr.kwater.hdcs.config.JwtTokenProvider;

import java.util.HashMap;
import java.util.Map;



@RestController
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    // 생성자를 통해 두 핵심 부품을 스프링으로부터 주입
    public AuthController(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/api/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        try {
            // 입력받은 아이디와 비밀번호로 인증 토큰 생성
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(username, password);

            // 인증 시작 
            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            // 인증 성공 시, JwtTokenProvider를 이용해 진짜 JWT 토큰을 발급
            String accessToken = jwtTokenProvider.createToken(authentication);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "로그인 성공!");
            response.put("user", authentication.getName());
            response.put("roles", authentication.getAuthorities());
            response.put("accessToken", accessToken); 

            return ResponseEntity.ok(response);

        } catch (AuthenticationException e) {
            // 아이디나 비밀번호가 틀렸을 때 예외 처리 (401 에러)
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "아이디 또는 비밀번호가 올바르지 않습니다.");
            return ResponseEntity.status(401).body(errorResponse);
        }
    }

    /**
     * ── JWT 토큰 검증 테스트용 API ──
     */
    @GetMapping("/api/test")
    public ResponseEntity<?> test() {
        return ResponseEntity.ok("[인증 성공] 유효한 JWT 토큰이 확인되었습니다. 보호된 데이터 열람이 허용됩니다.");
    }

}