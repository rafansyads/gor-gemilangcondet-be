package io.mpruy.gor_gemilangcondet.backend_api.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.Key;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtRoleExtractorTest {

    private static final String SECRET = "01234567890123456789012345678901";
    private final JwtRoleExtractor extractor = new JwtRoleExtractor(SECRET);

    private String bearerTokenWithRole(String role) {
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .claim("role", role)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
        return "Bearer " + token;
    }

    @Test
    @DisplayName("extractRole should return null when header is null")
    void extractRole_NullHeader() {
        assertNull(extractor.extractRole(null));
    }

    @Test
    @DisplayName("extractRole should return null when header is not bearer")
    void extractRole_NonBearerHeader() {
        assertNull(extractor.extractRole("Basic abc123"));
    }

    @Test
    @DisplayName("extractRole should return role from valid token")
    void extractRole_ValidToken() {
        String role = extractor.extractRole(bearerTokenWithRole("ADMIN"));

        assertEquals("ADMIN", role);
    }

    @Test
    @DisplayName("extractRole should return null for invalid token")
    void extractRole_InvalidToken() {
        assertNull(extractor.extractRole("Bearer invalid.token.value"));
    }

    @Test
    @DisplayName("canViewBookerName should allow ADMIN OWNER and STAF_LAPANGAN")
    void canViewBookerName_PrivilegedRoles() {
        assertTrue(extractor.canViewBookerName("ADMIN"));
        assertTrue(extractor.canViewBookerName("owner"));
        assertTrue(extractor.canViewBookerName("staf_lapangan"));
    }

    @Test
    @DisplayName("canViewBookerName should deny null and non-privileged roles")
    void canViewBookerName_NonPrivilegedRoles() {
        assertFalse(extractor.canViewBookerName(null));
        assertFalse(extractor.canViewBookerName("MEMBER"));
        assertFalse(extractor.canViewBookerName("GUEST"));
    }
}
