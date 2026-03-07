package io.mpruy.gor_gemilangcondet.backend_api.security.service;

import io.mpruy.gor_gemilangcondet.backend_api.exception.InvalidTokenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory refresh-token store.
 * Each username maps to a single active refresh token.
 * Replace the ConcurrentHashMap with a persistent store if the app needs to
 * survive restarts or run on multiple nodes.
 */
@Service
public class RefreshTokenService {

    // username → refresh-token value
    private final Map<String, String> tokenByUsername = new ConcurrentHashMap<>();

    // refresh-token value → username  (reverse lookup)
    private final Map<String, String> usernameByToken = new ConcurrentHashMap<>();

    @Value("${app.jwt.refresh-expiration:604800000}") // default 7 days
    private long refreshExpirationMs;

    // ─── Creation ──────────────────────────────────────────────────────────────

    /**
     * Creates (or replaces) a refresh token for the given username.
     *
     * @param username the authenticated user
     * @return the new opaque refresh token string
     */
    public String createRefreshToken(String username) {
        // Revoke any previous token for this user
        Optional.ofNullable(tokenByUsername.get(username))
                .ifPresent(old -> usernameByToken.remove(old));

        String token = UUID.randomUUID().toString();
        tokenByUsername.put(username, token);
        usernameByToken.put(token, username);
        return token;
    }

    // ─── Validation ────────────────────────────────────────────────────────────

    /**
     * Validates the given refresh token and returns the owning username.
     *
     * @param refreshToken opaque token string
     * @return owner username
     * @throws IllegalArgumentException if the token is unknown
     */
    public String validateRefreshToken(String refreshToken) {
        String username = usernameByToken.get(refreshToken);
        if (username == null) {
            throw new InvalidTokenException("Invalid or expired refresh token");
        }
        return username;
    }

    // ─── Revocation ────────────────────────────────────────────────────────────

    /**
     * Revokes the refresh token associated with the given username.
     *
     * @param username the user logging out
     */
    public void deleteRefreshTokenByUsername(String username) {
        String token = tokenByUsername.remove(username);
        if (token != null) {
            usernameByToken.remove(token);
        }
    }

    /**
     * Revokes a specific refresh token directly (used when only the token is available).
     *
     * @param refreshToken opaque token string
     */
    public void deleteRefreshToken(String refreshToken) {
        String username = usernameByToken.remove(refreshToken);
        if (username != null) {
            tokenByUsername.remove(username);
        }
    }
}
