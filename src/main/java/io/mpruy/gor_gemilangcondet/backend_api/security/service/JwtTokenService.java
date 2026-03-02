package io.mpruy.gor_gemilangcondet.backend_api.security.service;

import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.requests.LoginRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.requests.RefreshTokenRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.responses.AuthResponse;
import io.mpruy.gor_gemilangcondet.backend_api.security.UserDetailsImpl;
import io.mpruy.gor_gemilangcondet.backend_api.security.jwt.JwtUtils;
import io.mpruy.gor_gemilangcondet.backend_api.service.mapper.AuthMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final JwtTokenBlacklist jwtTokenBlacklist;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsService userDetailsService;
    private final AuthMapper authMapper;

    // ─── Login ─────────────────────────────────────────────────────────────────

    /**
     * Authenticates the user, issues an access token and a refresh token.
     *
     * @param request {@link LoginRequest} with username + password
     * @return {@link AuthResponse} containing both tokens
     */
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(), request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        String accessToken = jwtUtils.generateAccessToken(userDetails);
        String refreshToken = refreshTokenService.createRefreshToken(userDetails.getUsername());

        return authMapper.toAuthResponse(accessToken, refreshToken, userDetails);
    }

    // ─── Logout ────────────────────────────────────────────────────────────────

    /**
     * Blacklists the access token and revokes the refresh token for the given user.
     *
     * @param authorizationHeader value of the {@code Authorization} header ("Bearer <token>")
     */
    public void logout(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            jwtTokenBlacklist.blacklistToken(token);
            String username = jwtUtils.extractUsername(token);
            refreshTokenService.deleteRefreshTokenByUsername(username);
        }
        SecurityContextHolder.clearContext();
    }

    // ─── Refresh ───────────────────────────────────────────────────────────────

    /**
     * Validates the incoming refresh token, generates a new access token, and
     * returns an updated {@link AuthResponse} with the same refresh token.
     *
     * @param request {@link RefreshTokenRequest} with the existing refresh token
     * @return {@link AuthResponse} with a fresh access token
     */
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String username = refreshTokenService.validateRefreshToken(request.getRefreshToken());

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        String newAccessToken = jwtUtils.generateAccessToken(userDetails);

        String role = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("UNKNOWN");

        return authMapper.toRefreshedAuthResponse(
                newAccessToken, request.getRefreshToken(), username, role);
    }
}
