package io.mpruy.gor_gemilangcondet.backend_api.security.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory JWT token blacklist.
 * Tokens are added here on logout and checked by {@link io.mpruy.gor_gemilangcondet.backend_api.security.jwt.JwtTokenFilter}
 * before trusting any incoming access token.
 */
@Service
public class JwtTokenBlacklist {

    private final Set<String> blacklistedTokens =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Adds a token to the blacklist so it is rejected on all future requests.
     *
     * @param token raw JWT string (without "Bearer " prefix)
     */
    public void blacklistToken(String token) {
        blacklistedTokens.add(token);
    }

    /**
     * Returns {@code true} if the given token has been blacklisted.
     *
     * @param token raw JWT string
     */
    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }
}
