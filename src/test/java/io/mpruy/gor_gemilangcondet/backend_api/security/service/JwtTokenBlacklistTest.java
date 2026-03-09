package io.mpruy.gor_gemilangcondet.backend_api.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenBlacklistTest {

    private JwtTokenBlacklist blacklist;

    @BeforeEach
    void setUp() {
        blacklist = new JwtTokenBlacklist();
    }

    @Test
    @DisplayName("Should blacklist a token")
    void blacklistToken() {
        blacklist.blacklistToken("token123");

        assertTrue(blacklist.isTokenBlacklisted("token123"));
    }

    @Test
    @DisplayName("Should return false for non-blacklisted token")
    void nonBlacklistedToken() {
        assertFalse(blacklist.isTokenBlacklisted("sometoken"));
    }

    @Test
    @DisplayName("Should handle multiple tokens")
    void multipleTokens() {
        blacklist.blacklistToken("token1");
        blacklist.blacklistToken("token2");

        assertTrue(blacklist.isTokenBlacklisted("token1"));
        assertTrue(blacklist.isTokenBlacklisted("token2"));
        assertFalse(blacklist.isTokenBlacklisted("token3"));
    }
}
