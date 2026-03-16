package io.mpruy.gor_gemilangcondet.backend_api.service.mapper;

import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.responses.AuthResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.Role;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.RoleName;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.User;
import io.mpruy.gor_gemilangcondet.backend_api.security.UserDetailsImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuthMapperTest {

    private final AuthMapper authMapper = new AuthMapper();

    @Test
    @DisplayName("Should map to AuthResponse from UserDetails")
    void toAuthResponse_Success() {
        Role role = new Role();
        role.setId(1);
        role.setRoleName(RoleName.MEMBER);
        User user = User.builder()
                .id(UUID.randomUUID()).username("testuser")
                .email("test@test.com").password("Pass1234").role(role)
                .build();
        UserDetailsImpl userDetails = new UserDetailsImpl(user);

        AuthResponse result = authMapper.toAuthResponse("access-token", "refresh-token", userDetails);

        assertEquals("access-token", result.getAccessToken());
        assertEquals("refresh-token", result.getRefreshToken());
        assertEquals("Bearer", result.getTokenType());
        assertEquals("testuser", result.getUsername());
        assertEquals("MEMBER", result.getRole());
    }

    @Test
    @DisplayName("Should map to refreshed AuthResponse")
    void toRefreshedAuthResponse_Success() {
        AuthResponse result = authMapper.toRefreshedAuthResponse(
                "new-access", "refresh-token", "testuser", "ADMIN");

        assertEquals("new-access", result.getAccessToken());
        assertEquals("refresh-token", result.getRefreshToken());
        assertEquals("Bearer", result.getTokenType());
        assertEquals("testuser", result.getUsername());
        assertEquals("ADMIN", result.getRole());
    }
}
