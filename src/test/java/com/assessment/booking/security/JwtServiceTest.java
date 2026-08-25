package com.assessment.booking.security;

import com.assessment.booking.entity.Role;
import com.assessment.booking.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private final String testSecret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long testExpiration = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", testSecret);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", testExpiration);
    }

    @Test
    @DisplayName("Should generate valid JWT token and extract correct claims")
    void testGenerateAndValidateToken() {
        User user = User.builder()
                .id(1L)
                .email("john.doe@example.com")
                .fullName("John Doe")
                .role(Role.ROLE_USER)
                .build();

        String token = jwtService.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("john.doe@example.com");
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    @DisplayName("Should correctly extract extra claims from JWT token")
    void testExtractExtraClaims() {
        User admin = User.builder()
                .id(99L)
                .email("admin@enterprise.com")
                .fullName("Admin User")
                .role(Role.ROLE_ADMIN)
                .build();

        String token = jwtService.generateToken(admin);

        var claims = jwtService.extractAllClaims(token);
        assertThat(claims.get("userId", Long.class)).isEqualTo(99L);
        assertThat(claims.get("role", String.class)).isEqualTo("ROLE_ADMIN");
        assertThat(claims.get("fullName", String.class)).isEqualTo("Admin User");
    }
}
