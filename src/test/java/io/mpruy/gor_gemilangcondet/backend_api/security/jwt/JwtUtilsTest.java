package io.mpruy.gor_gemilangcondet.backend_api.security.jwt;

import io.mpruy.gor_gemilangcondet.backend_api.entities.users.Role;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.RoleName;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.User;
import io.mpruy.gor_gemilangcondet.backend_api.security.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret",
                "TestSecretKeyForJwtTokensThatMustBeLongEnough1234567890");
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 86400000L);

        Role role = new Role();
        role.setId(1);
        role.setRoleName(RoleName.MEMBER);
        User user = User.builder()
                .id(UUID.randomUUID()).username("testuser")
                .email("test@test.com").password("Pass1234").role(role)
                .build();
        userDetails = new UserDetailsImpl(user);
    }

    @Nested
    @DisplayName("Token Generation Tests")
    class GenerationTests {

        @Test
        @DisplayName("Should generate a valid access token")
        void generateAccessToken_Success() {
            String token = jwtUtils.generateAccessToken(userDetails);

            assertNotNull(token);
            assertFalse(token.isEmpty());
        }

        @Test
        @DisplayName("Token should contain correct username")
        void generateAccessToken_ContainsUsername() {
            String token = jwtUtils.generateAccessToken(userDetails);

            assertEquals("testuser", jwtUtils.extractUsername(token));
        }

        @Test
        @DisplayName("Token should contain correct role")
        void generateAccessToken_ContainsRole() {
            String token = jwtUtils.generateAccessToken(userDetails);

            assertEquals("MEMBER", jwtUtils.extractRole(token));
        }
    }

    @Nested
    @DisplayName("Token Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should validate a correct token")
        void validateToken_Valid() {
            String token = jwtUtils.generateAccessToken(userDetails);

            assertTrue(jwtUtils.validateToken(token, userDetails));
        }

        @Test
        @DisplayName("Should reject token for wrong user")
        void validateToken_WrongUser() {
            String token = jwtUtils.generateAccessToken(userDetails);

            Role role = new Role();
            role.setId(1);
            role.setRoleName(RoleName.GUEST);
            User otherUser = User.builder()
                    .id(UUID.randomUUID()).username("otheruser")
                    .email("other@test.com").password("Pass1234").role(role)
                    .build();
            UserDetailsImpl otherDetails = new UserDetailsImpl(otherUser);

            assertFalse(jwtUtils.validateToken(token, otherDetails));
        }

        @Test
        @DisplayName("Should reject expired token")
        void validateToken_Expired() {
            ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", -1000L);
            String token = jwtUtils.generateAccessToken(userDetails);
            ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 86400000L);

            assertFalse(jwtUtils.validateToken(token, userDetails));
        }

        @Test
        @DisplayName("Should reject malformed token")
        void validateToken_Malformed() {
            assertFalse(jwtUtils.validateToken("not.a.token", userDetails));
        }

        @Test
        @DisplayName("Should detect expired token")
        void isTokenExpired_True() {
            ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", -1000L);
            String token = jwtUtils.generateAccessToken(userDetails);
            ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 86400000L);

            assertThrows(io.jsonwebtoken.ExpiredJwtException.class, () -> jwtUtils.isTokenExpired(token));
        }

        @Test
        @DisplayName("Should detect non-expired token")
        void isTokenExpired_False() {
            String token = jwtUtils.generateAccessToken(userDetails);

            assertFalse(jwtUtils.isTokenExpired(token));
        }
    }

    @Nested
    @DisplayName("Claims Extraction Tests")
    class ExtractionTests {

        @Test
        @DisplayName("Should extract username from token")
        void extractUsername() {
            String token = jwtUtils.generateAccessToken(userDetails);

            assertEquals("testuser", jwtUtils.extractUsername(token));
        }

        @Test
        @DisplayName("Should extract expiration from token")
        void extractExpiration() {
            String token = jwtUtils.generateAccessToken(userDetails);

            assertNotNull(jwtUtils.extractExpiration(token));
        }

        @Test
        @DisplayName("Should extract role from token")
        void extractRole() {
            String token = jwtUtils.generateAccessToken(userDetails);

            assertEquals("MEMBER", jwtUtils.extractRole(token));
        }
    }
}
