package io.mpruy.gor_gemilangcondet.backend_api.security.service;

import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.requests.LoginRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.requests.RefreshTokenRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.responses.AuthResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.Role;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.RoleName;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.User;
import io.mpruy.gor_gemilangcondet.backend_api.security.UserDetailsImpl;
import io.mpruy.gor_gemilangcondet.backend_api.security.jwt.JwtUtils;
import io.mpruy.gor_gemilangcondet.backend_api.service.mapper.AuthMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private JwtTokenBlacklist jwtTokenBlacklist;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private AuthMapper authMapper;

    @InjectMocks
    private JwtTokenService jwtTokenService;

    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        Role role = new Role();
        role.setId(1);
        role.setRoleName(RoleName.MEMBER);
        User user = User.builder()
                .id(UUID.randomUUID()).username("testuser")
                .email("test@test.com").password("Pass1234").role(role)
                .build();
        userDetails = new UserDetailsImpl(user);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should login successfully")
    void login_Success() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("Pass1234");

        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtUtils.generateAccessToken(userDetails)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken("testuser")).thenReturn("refresh-token");
        when(authMapper.toAuthResponse("access-token", "refresh-token", userDetails))
                .thenReturn(AuthResponse.builder().accessToken("access-token").refreshToken("refresh-token").build());

        AuthResponse result = jwtTokenService.login(loginRequest);

        assertNotNull(result);
        assertEquals("access-token", result.getAccessToken());
        assertEquals("refresh-token", result.getRefreshToken());
    }

    @Test
    @DisplayName("Should logout and blacklist token")
    void logout_Success() {
        when(jwtUtils.extractUsername("some-jwt-token")).thenReturn("testuser");

        jwtTokenService.logout("Bearer some-jwt-token");

        verify(jwtTokenBlacklist).blacklistToken("some-jwt-token");
        verify(refreshTokenService).deleteRefreshTokenByUsername("testuser");
    }

    @Test
    @DisplayName("Should handle logout with null header")
    void logout_NullHeader() {
        jwtTokenService.logout(null);

        verify(jwtTokenBlacklist, never()).blacklistToken(any());
    }

    @Test
    @DisplayName("Should handle logout with non-Bearer header")
    void logout_NonBearerHeader() {
        jwtTokenService.logout("Basic abc123");

        verify(jwtTokenBlacklist, never()).blacklistToken(any());
    }

    @Test
    @DisplayName("Should refresh token successfully")
    void refreshToken_Success() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("old-refresh-token");

        when(refreshTokenService.validateRefreshToken("old-refresh-token")).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtUtils.generateAccessToken(userDetails)).thenReturn("new-access-token");
        when(authMapper.toRefreshedAuthResponse("new-access-token", "old-refresh-token", "testuser", "MEMBER"))
                .thenReturn(AuthResponse.builder().accessToken("new-access-token").refreshToken("old-refresh-token")
                        .build());

        AuthResponse result = jwtTokenService.refreshToken(request);

        assertNotNull(result);
        assertEquals("new-access-token", result.getAccessToken());
    }
}
