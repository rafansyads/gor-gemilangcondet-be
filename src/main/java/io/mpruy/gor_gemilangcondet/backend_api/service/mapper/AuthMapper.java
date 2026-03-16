package io.mpruy.gor_gemilangcondet.backend_api.service.mapper;

import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.responses.AuthResponse;
import io.mpruy.gor_gemilangcondet.backend_api.security.UserDetailsImpl;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    /**
     * Builds an {@link AuthResponse} from a pair of tokens and the authenticated user details.
     *
     * @param accessToken  freshly-generated JWT access token
     * @param refreshToken opaque refresh token
     * @param userDetails  the authenticated principal
     * @return populated {@link AuthResponse}
     */
    public AuthResponse toAuthResponse(String accessToken, String refreshToken, UserDetailsImpl userDetails) {
        String role = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("UNKNOWN");

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .username(userDetails.getUsername())
                .role(role)
                .build();
    }

    /**
     * Builds an {@link AuthResponse} when a refresh produces a new access token
     * but reuses the same refresh token.
     *
     * @param newAccessToken newly-generated JWT access token
     * @param refreshToken   existing refresh token (unchanged)
     * @param username       owner username
     * @param role           owner role authority string
     * @return populated {@link AuthResponse}
     */
    public AuthResponse toRefreshedAuthResponse(String newAccessToken, String refreshToken,
                                                String username, String role) {
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .username(username)
                .role(role)
                .build();
    }
}
