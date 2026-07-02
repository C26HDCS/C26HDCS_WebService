package kr.kwater.hdcs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider; 

    public SecurityConfig(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // URL별 권한 설정
            .authorizeHttpRequests(auth -> auth
                // 1. 누구나 접근 가능한 URL (인증 불필요)
                .antMatchers(
                    "/", "/index", "/error", 
                    "/api/login", "/login", "/logout",
                    "/register", "/find-id", "/find-password",
                    "/static/**", "/webjars/**",
                    "/h2-console/**",
                    "/api/dashboard/**",
                    "/api/collectionSetting/**",
                    
                    // 화면(View) 이동을 위한 URL 라우팅 허용
                    "/dashboard", "/output", "/distribution", "/ftp", "/user", 
                    "/mypage", "/alarm", "/history", "/statistics", "/intro", "/collectionSetting"
                ).permitAll()
                
                // 2. 인증(JWT 토큰)이 반드시 필요한 URL 명시적 설정 (옵션)
                // /api/user/** 밑으로 들어오는 모든 API는 인증을 요구함
                .antMatchers("/api/user/**").authenticated() 

                // 3. 그 외에 명시되지 않은 모든 요청도 기본적으로 인증 필요
                .anyRequest().authenticated()
            )

            // H2 Console 프레임 허용 
            .headers(headers -> headers.frameOptions().sameOrigin())

            // ── JWT 인증 필터 적용 ──────────────────────────────────
            // UsernamePasswordAuthenticationFilter 전에 AccessTokenFilter가 먼저 실행되어 JWT를 검사합니다.
            .addFilterBefore(new AccessTokenFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 인증 매니저(AuthenticationManager) 등록
     * 로그인 API에서 아이디/비밀번호 검증 시 이 객체를 사용
     */
    @Bean
    public AuthenticationManager authenticationManager(
            org.springframework.security.core.userdetails.UserDetailsService userDetailsService, 
            PasswordEncoder passwordEncoder) {
        
        org.springframework.security.authentication.dao.DaoAuthenticationProvider provider = 
                new org.springframework.security.authentication.dao.DaoAuthenticationProvider();
        
        provider.setUserDetailsService(userDetailsService); 
        provider.setPasswordEncoder(passwordEncoder);      
        
        return new org.springframework.security.authentication.ProviderManager(provider);
    }
}