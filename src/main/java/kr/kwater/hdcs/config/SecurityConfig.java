package kr.kwater.hdcs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) -> {
                    response.setStatus(401);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":\"AUTHENTICATION_REQUIRED\"}");
                })
                .accessDeniedHandler((request, response, exception) -> {
                    response.setStatus(403);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":\"ACCESS_DENIED\"}");
                })
            )

            // URL별 권한 설정
            .authorizeHttpRequests(auth -> auth
                // 0. 관리자 전용 화면(partial) 요청. SPA가 화면 조각을 가져올 때(X-Requested-With 헤더)만 검사한다.
                //    주소창 직접 접속은 SPA 껍데기만 내려가고, 화면 이동은 프론트 라우트 가드가 막는다.
                //    (permitAll 보다 먼저 선언해야 한다)
                .requestMatchers(SecurityConfig::isAdminPartialRequest).hasRole("ADMIN")

                // 1. 누구나 접근 가능한 URL (인증 불필요)
                .antMatchers(
                    "/", "/index", "/error",
                    "/api/login", "/login", "/logout",
                    "/register", "/find-id", "/find-password",
                    "/api/public/**",
                    "/static/**", "/webjars/**",
                    "/h2-console/**",
                    "/api/dashboard/**",
                    "/api/collectionSetting/**",
                    
                    // 화면(View) 이동을 위한 URL 라우팅 허용
                    "/dashboard", "/output", "/distribution", "/ftp", "/user", "/report",
                    "/mypage", "/log", "/alarm", "/config", "/storage", "/history", "/statistics", "/intro", "/collectionSetting"
                ).permitAll()
                
                // 2. 관리자(ADMIN) 전용 API: 환경설정/저장장치/사용자 관리, 리포트 수동 생성
                //    일반 사용자(USER)는 토큰이 있어도 403
                .antMatchers("/api/user/**", "/api/config/**", "/api/storage/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.POST, "/api/report", "/api/report/**").hasRole("ADMIN")

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

    /** 관리자 전용 화면 경로 (SPA partial 요청에만 권한을 검사한다) */
    private static final java.util.Set<String> ADMIN_PAGES = java.util.Set.of("/user", "/config", "/storage");

    private static boolean isAdminPartialRequest(javax.servlet.http.HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"))
                && ADMIN_PAGES.contains(request.getServletPath());
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
