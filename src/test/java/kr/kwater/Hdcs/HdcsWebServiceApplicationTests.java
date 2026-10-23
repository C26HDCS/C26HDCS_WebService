package kr.kwater.Hdcs;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
class HdcsWebServiceApplicationTests {
    @org.springframework.beans.factory.annotation.Autowired
    javax.sql.DataSource dataSource;

    @org.springframework.beans.factory.annotation.Autowired
    org.springframework.test.web.servlet.MockMvc web;
    @org.springframework.beans.factory.annotation.Autowired
    kr.kwater.hdcs.config.JwtTokenProvider tokens;
    @org.springframework.beans.factory.annotation.Value("${jwt.secret}")
    String secret;

    @Test void protectedApisReturn401ForMissingInvalidAndExpiredTokens() throws Exception {
        var expired = io.jsonwebtoken.Jwts.builder().setSubject("test_admin")
                .claim("auth", "ROLE_ADMIN").setExpiration(new java.util.Date(1))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(io.jsonwebtoken.io.Decoders.BASE64.decode(secret)))
                .compact();
        for (String token : java.util.List.of("", "invalid", expired)) {
            web.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/config/groups")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code")
                            .value("AUTHENTICATION_REQUIRED"));
        }
        var token = tokens.createToken(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "test_admin", "", java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))));
        web.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/config/groups")
                .header("Authorization", "Bearer " + token))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }

    @Test void configScreenShellIsAvailableWithoutExposingItsApi() throws Exception {
        web.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/config")
                .servletPath("/config").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.view().name("config/config"));
    }

    @Test
    void contextLoads() throws Exception {
        try (var connection = dataSource.getConnection()) {
            org.assertj.core.api.Assertions.assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:");
            try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT COUNT(*) FROM TB_DEVICE")) {
                rows.next();
                org.assertj.core.api.Assertions.assertThat(rows.getInt(1)).isEqualTo(2);
            }
        }
    }

}
