package io.mpruy.gor_gemilangcondet.backend_api.security.service;

import io.mpruy.gor_gemilangcondet.backend_api.exception.InvalidTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RefreshTokenServiceTest {

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService();
    }

    @Test
    @DisplayName("Should create refresh token")
    void createRefreshToken_Success() {
        String token = refreshTokenService.createRefreshToken("testuser");

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("Should validate valid refresh token")
    void validateRefreshToken_Success() {
        String token = refreshTokenService.createRefreshToken("testuser");

        String username = refreshTokenService.validateRefreshToken(token);

        assertEquals("testuser", username);
    }

    @Test
    @DisplayName("Should throw for invalid refresh token")
    void validateRefreshToken_Invalid() {
        assertThrows(InvalidTokenException.class,
                () -> refreshTokenService.validateRefreshToken("fake-token"));
    }

    @Test
    @DisplayName("Should replace previous token on re-creation")
    void createRefreshToken_ReplacePrevious() {
        String token1 = refreshTokenService.createRefreshToken("testuser");
        String token2 = refreshTokenService.createRefreshToken("testuser");

        assertNotEquals(token1, token2);
        assertThrows(InvalidTokenException.class,
                () -> refreshTokenService.validateRefreshToken(token1));
        assertEquals("testuser", refreshTokenService.validateRefreshToken(token2));
    }

    @Test
    @DisplayName("Should delete refresh token by username")
    void deleteByUsername() {
        String token = refreshTokenService.createRefreshToken("testuser");

        refreshTokenService.deleteRefreshTokenByUsername("testuser");

        assertThrows(InvalidTokenException.class,
                () -> refreshTokenService.validateRefreshToken(token));
    }

    @Test
    @DisplayName("Should delete refresh token directly")
    void deleteDirectly() {
        String token = refreshTokenService.createRefreshToken("testuser");

        refreshTokenService.deleteRefreshToken(token);

        assertThrows(InvalidTokenException.class,
                () -> refreshTokenService.validateRefreshToken(token));
    }

    @Test
    @DisplayName("Should handle deleting non-existent token by username gracefully")
    void deleteByUsername_NonExistent() {
        assertDoesNotThrow(() -> refreshTokenService.deleteRefreshTokenByUsername("unknown"));
    }

    @Test
    @DisplayName("Should handle deleting non-existent token directly gracefully")
    void deleteDirectly_NonExistent() {
        assertDoesNotThrow(() -> refreshTokenService.deleteRefreshToken("unknown-token"));
    }
}
