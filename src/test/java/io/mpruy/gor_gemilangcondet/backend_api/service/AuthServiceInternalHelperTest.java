package io.mpruy.gor_gemilangcondet.backend_api.service;

import io.mpruy.gor_gemilangcondet.backend_api.entities.users.RoleName;
import io.mpruy.gor_gemilangcondet.backend_api.exception.BadRequestException;
import io.mpruy.gor_gemilangcondet.backend_api.repository.RoleRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserStatusRepository;
import io.mpruy.gor_gemilangcondet.backend_api.security.jwt.JwtUtils;
import io.mpruy.gor_gemilangcondet.backend_api.security.service.JwtTokenBlacklist;
import io.mpruy.gor_gemilangcondet.backend_api.security.service.RefreshTokenService;
import io.mpruy.gor_gemilangcondet.backend_api.service.mapper.AuthMapper;
import io.mpruy.gor_gemilangcondet.backend_api.service.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class AuthServiceInternalHelperTest {

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
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserStatusRepository userStatusRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("parseUserLockedUntil should handle null and past timestamps")
    void parseUserLockedUntil_NullAndPast() {
        String unknown = ReflectionTestUtils.invokeMethod(authService, "parseUserLockedUntil", (Object) null);
        String past = ReflectionTestUtils.invokeMethod(authService, "parseUserLockedUntil", LocalDateTime.now().minusMinutes(1));

        assertEquals("waktu tidak diketahui", unknown);
        assertEquals("baru saja", past);
    }

    @Test
    @DisplayName("parseUserLockedUntil should format seconds window")
    void parseUserLockedUntil_Seconds() {
        String value = ReflectionTestUtils.invokeMethod(authService, "parseUserLockedUntil", LocalDateTime.now().plusSeconds(30));

        assertTrue(value.contains("detik"));
    }

    @Test
    @DisplayName("parseUserLockedUntil should format minutes and trailing seconds")
    void parseUserLockedUntil_Minutes() {
        String value = ReflectionTestUtils.invokeMethod(authService, "parseUserLockedUntil", LocalDateTime.now().plusMinutes(3).plusSeconds(10));

        assertTrue(value.contains("menit"));
        assertTrue(value.contains("detik"));
    }

    @Test
    @DisplayName("parseUserLockedUntil should format hours")
    void parseUserLockedUntil_Hours() {
        String value = ReflectionTestUtils.invokeMethod(authService, "parseUserLockedUntil", LocalDateTime.now().plusHours(2).plusMinutes(5));

        assertTrue(value.contains("jam"));
    }

    @Test
    @DisplayName("parseUserLockedUntil should format days")
    void parseUserLockedUntil_Days() {
        String value = ReflectionTestUtils.invokeMethod(authService, "parseUserLockedUntil", LocalDateTime.now().plusDays(2).plusHours(3));

        assertTrue(value.contains("hari"));
    }

    @Test
    @DisplayName("parseAdminRole should parse allowed role")
    void parseAdminRole_AllowedRole() {
        RoleName roleName = ReflectionTestUtils.invokeMethod(authService, "parseAdminRole", "admin");

        assertEquals(RoleName.ADMIN, roleName);
    }

    @Test
    @DisplayName("parseAdminRole should reject missing disallowed and invalid role")
    void parseAdminRole_InvalidCases() {
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(authService, "parseAdminRole", (Object) null));
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(authService, "parseAdminRole", "member"));
        assertThrows(BadRequestException.class, () -> ReflectionTestUtils.invokeMethod(authService, "parseAdminRole", "invalid-role"));
    }
}
