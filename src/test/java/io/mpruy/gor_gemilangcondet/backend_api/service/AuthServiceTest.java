package io.mpruy.gor_gemilangcondet.backend_api.service;

import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.requests.LoginRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.requests.RefreshTokenRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.requests.RegisterRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.responses.AuthResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.responses.RegisterResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.Role;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.RoleName;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.User;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatusName;
import io.mpruy.gor_gemilangcondet.backend_api.exception.BadRequestException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ConflictException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ForbiddenException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.TooManyRequestException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.UnauthorizedException;
import io.mpruy.gor_gemilangcondet.backend_api.repository.RoleRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserStatusRepository;
import io.mpruy.gor_gemilangcondet.backend_api.security.UserDetailsImpl;
import io.mpruy.gor_gemilangcondet.backend_api.security.jwt.JwtUtils;
import io.mpruy.gor_gemilangcondet.backend_api.security.service.JwtTokenBlacklist;
import io.mpruy.gor_gemilangcondet.backend_api.security.service.RefreshTokenService;
import io.mpruy.gor_gemilangcondet.backend_api.service.mapper.AuthMapper;
import io.mpruy.gor_gemilangcondet.backend_api.service.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

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

    private User testUser;
    private Role guestRole;
    private UserDetailsImpl userDetails;
    private UserStatus activeStatus;
    private UserStatus pendingStatus;
    private UserStatus nonActiveStatus;

    @BeforeEach
    void setUp() {
        guestRole = Role.builder().id(1).roleName(RoleName.GUEST).build();
        activeStatus = UserStatus.builder().id(1).name(UserStatusName.AKTIF).build();
        pendingStatus = UserStatus.builder().id(2).name(UserStatusName.PENDING).build();
        nonActiveStatus = UserStatus.builder().id(3).name(UserStatusName.NON_AKTIF).build();
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .role(guestRole)
                .status(activeStatus)
                .build();
        userDetails = new UserDetailsImpl(testUser);

        lenient().when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        lenient().when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        lenient().when(userStatusRepository.findByName(UserStatusName.AKTIF)).thenReturn(Optional.of(activeStatus));
        lenient().when(userStatusRepository.findByName(UserStatusName.PENDING)).thenReturn(Optional.of(pendingStatus));
        lenient().when(userStatusRepository.findByName(UserStatusName.NON_AKTIF))
                .thenReturn(Optional.of(nonActiveStatus));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LOGIN
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should reject login when persisted failed login attempts are still blocked")
        void login_UserFailedAttemptsBlocked_ThrowsTooManyRequest() {
            LoginRequest request = new LoginRequest();
            request.setUsernameOrEmail("testuser");
            request.setPassword("Password1");

            testUser.setFailedLoginAttempts(5);
            testUser.setFailedLoginWindowStartedAt(LocalDateTime.now().minusSeconds(10));
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            assertThrows(TooManyRequestException.class, () -> authService.login(request, null));
            verify(authenticationManager, never()).authenticate(any());
        }

        @Test
        @DisplayName("Should persist failed login attempt to user when authentication fails")
        void login_UserBadCredentials_ShouldPersistFailedAttempt() {
            LoginRequest request = new LoginRequest();
            request.setUsernameOrEmail("testuser");
            request.setPassword("wrong");

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThrows(BadCredentialsException.class, () -> authService.login(request, null));
            verify(userRepository, atLeastOnce()).save(argThat(u -> u.getFailedLoginAttempts() >= 1));
            verify(loginAttemptService, never()).recordFailedAttempt("testuser");
        }

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void login_Success() {
            LoginRequest request = new LoginRequest();
            request.setUsernameOrEmail("testuser");
            request.setPassword("Password1");

            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn(userDetails);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(auth);
            when(jwtUtils.generateAccessToken(userDetails)).thenReturn("access-token");
            when(refreshTokenService.createRefreshToken("testuser")).thenReturn("refresh-token");

            AuthResponse expectedResponse = AuthResponse.builder()
                    .accessToken("access-token")
                    .refreshToken("refresh-token")
                    .username("testuser")
                    .role("GUEST")
                    .build();
            when(authMapper.toAuthResponse("access-token", "refresh-token", userDetails))
                    .thenReturn(expectedResponse);

            AuthResponse result = authService.login(request, "/dashboard");

            assertNotNull(result);
            assertEquals("access-token", result.getAccessToken());
            assertEquals("/dashboard", result.getRedirectUrl());
        }

        @Test
        @DisplayName("Should login successfully without redirect URL")
        void login_SuccessWithoutRedirectUrl() {
            LoginRequest request = new LoginRequest();
            request.setUsernameOrEmail("testuser");
            request.setPassword("Password1");

            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn(userDetails);
            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(jwtUtils.generateAccessToken(userDetails)).thenReturn("token");
            when(refreshTokenService.createRefreshToken("testuser")).thenReturn("refresh");

            AuthResponse resp = AuthResponse.builder()
                    .accessToken("token").refreshToken("refresh").username("testuser").role("GUEST").build();
            when(authMapper.toAuthResponse("token", "refresh", userDetails)).thenReturn(resp);

            AuthResponse result = authService.login(request, null);

            assertNotNull(result);
            assertNull(result.getRedirectUrl());
        }

        @Test
        @DisplayName("Should throw exception for invalid credentials")
        void login_InvalidCredentials() {
            LoginRequest request = new LoginRequest();
            request.setUsernameOrEmail("testuser");
            request.setPassword("wrong");

            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThrows(BadCredentialsException.class, () -> authService.login(request, null));
        }

        @Test
        @DisplayName("Should reject pending user login with forbidden")
        void login_PendingUser_Forbidden() {
            LoginRequest request = new LoginRequest();
            request.setUsernameOrEmail("pendinguser");
            request.setPassword("Password1");

            User pendingUser = User.builder()
                    .id(UUID.randomUUID())
                    .username("pendinguser")
                    .email("pending@example.com")
                    .password("encoded")
                    .role(guestRole)
                    .status(pendingStatus)
                    .build();

            when(userRepository.findByUsername("pendinguser")).thenReturn(Optional.of(pendingUser));

            assertThrows(ForbiddenException.class, () -> authService.login(request, null));
            verify(authenticationManager, never()).authenticate(any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LOGIN ADMIN
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Login Admin Tests")
    class LoginAdminTests {

        @Test
        @DisplayName("Should login admin with ADMIN role")
        void loginAdmin_SuccessWithAdminRole() {
            Role adminRole = Role.builder().id(6).roleName(RoleName.ADMIN).build();
            User adminUser = User.builder()
                    .id(UUID.randomUUID()).username("admin1").email("admin@test.com")
                    .password("enc").role(adminRole).status(activeStatus).build();
            UserDetailsImpl adminDetails = new UserDetailsImpl(adminUser);

            LoginRequest request = new LoginRequest();
            request.setUsernameOrEmail("admin1");
            request.setPassword("Password1");

            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn(adminDetails);
            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(jwtUtils.generateAccessToken(adminDetails)).thenReturn("admin-token");
            when(refreshTokenService.createRefreshToken("admin1")).thenReturn("admin-refresh");

            AuthResponse resp = AuthResponse.builder()
                    .accessToken("admin-token").refreshToken("admin-refresh")
                    .username("admin1").role("ADMIN").build();
            when(authMapper.toAuthResponse("admin-token", "admin-refresh", adminDetails)).thenReturn(resp);

            AuthResponse result = authService.loginAdmin(request, null);

            assertEquals("ADMIN", result.getRole());
        }

        @Test
        @DisplayName("Should login admin with STAF_LAPANGAN role")
        void loginAdmin_SuccessWithStafRole() {
            Role stafRole = Role.builder().id(3).roleName(RoleName.STAF_LAPANGAN).build();
            User stafUser = User.builder()
                    .id(UUID.randomUUID()).username("staf1").email("staf@test.com")
                    .password("enc").role(stafRole).status(activeStatus).build();
            UserDetailsImpl stafDetails = new UserDetailsImpl(stafUser);

            LoginRequest request = new LoginRequest();
            request.setUsernameOrEmail("staf1");
            request.setPassword("Password1");

            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn(stafDetails);
            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(jwtUtils.generateAccessToken(stafDetails)).thenReturn("staf-token");
            when(refreshTokenService.createRefreshToken("staf1")).thenReturn("staf-refresh");

            AuthResponse resp = AuthResponse.builder()
                    .accessToken("staf-token").refreshToken("staf-refresh")
                    .username("staf1").role("STAF_LAPANGAN").build();
            when(authMapper.toAuthResponse("staf-token", "staf-refresh", stafDetails)).thenReturn(resp);

            AuthResponse result = authService.loginAdmin(request, null);

            assertEquals("STAF_LAPANGAN", result.getRole());
        }

        @Test
        @DisplayName("Should reject login admin for GUEST role")
        void loginAdmin_RejectGuestRole() {
            LoginRequest request = new LoginRequest();
            request.setUsernameOrEmail("testuser");
            request.setPassword("Password1");

            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn(userDetails);
            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(jwtUtils.generateAccessToken(userDetails)).thenReturn("token");
            when(refreshTokenService.createRefreshToken("testuser")).thenReturn("refresh");

            AuthResponse resp = AuthResponse.builder()
                    .accessToken("token").refreshToken("refresh").username("testuser").role("GUEST").build();
            when(authMapper.toAuthResponse("token", "refresh", userDetails)).thenReturn(resp);

            assertThrows(UnauthorizedException.class, () -> authService.loginAdmin(request, null));
        }

        @Test
        @DisplayName("Should reject login admin for MEMBER role")
        void loginAdmin_RejectMemberRole() {
            Role memberRole = Role.builder().id(2).roleName(RoleName.MEMBER).build();
            User memberUser = User.builder()
                    .id(UUID.randomUUID()).username("member1").email("m@test.com")
                    .password("enc").role(memberRole).status(activeStatus).build();
            UserDetailsImpl memberDetails = new UserDetailsImpl(memberUser);

            LoginRequest request = new LoginRequest();
            request.setUsernameOrEmail("member1");
            request.setPassword("Password1");

            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn(memberDetails);
            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(jwtUtils.generateAccessToken(memberDetails)).thenReturn("token");
            when(refreshTokenService.createRefreshToken("member1")).thenReturn("refresh");

            AuthResponse resp = AuthResponse.builder()
                    .accessToken("token").refreshToken("refresh").username("member1").role("MEMBER").build();
            when(authMapper.toAuthResponse("token", "refresh", memberDetails)).thenReturn(resp);

            assertThrows(UnauthorizedException.class, () -> authService.loginAdmin(request, null));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // REGISTER
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Register Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should register new user successfully")
        void register_Success() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setEmail("new@example.com");
            request.setPassword("Password1");

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(roleRepository.findByRoleName(RoleName.GUEST)).thenReturn(Optional.of(guestRole));
            when(passwordEncoder.encode("Password1")).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(i -> {
                User u = i.getArgument(0);
                u.setId(UUID.randomUUID());
                return u;
            });
            when(userMapper.toDto(any(User.class))).thenReturn(null);

            RegisterResponse result = authService.register(request);

            assertNotNull(result);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should set AKTIF status for public registration")
        void register_ShouldSetAktifStatus() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setEmail("new@example.com");
            request.setPassword("Password1");

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(roleRepository.findByRoleName(RoleName.GUEST)).thenReturn(Optional.of(guestRole));
            when(passwordEncoder.encode("Password1")).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
            when(userMapper.toDto(any(User.class))).thenReturn(null);

            authService.register(request);

            verify(userRepository).save(argThat(
                    user -> user.getStatus() != null && UserStatusName.AKTIF.equals(user.getStatus().getName())));
        }

        @Test
        @DisplayName("Should reject registration with duplicate username")
        void register_DuplicateUsername() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("existing");
            request.setEmail("new@example.com");
            request.setPassword("Password1");

            when(userRepository.existsByUsername("existing")).thenReturn(true);

            assertThrows(ConflictException.class, () -> authService.register(request));
        }

        @Test
        @DisplayName("Should reject registration with duplicate email")
        void register_DuplicateEmail() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setEmail("existing@example.com");
            request.setPassword("Password1");

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            assertThrows(ConflictException.class, () -> authService.register(request));
        }

        @Test
        @DisplayName("Should reject short password")
        void register_ShortPassword() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setEmail("new@example.com");
            request.setPassword("Pass1");

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

            assertThrows(BadRequestException.class, () -> authService.register(request));
        }

        @Test
        @DisplayName("Should reject password longer than 20 chars")
        void register_TooLongPassword() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setEmail("new@example.com");
            request.setPassword("Password1234567890123");

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

            assertThrows(BadRequestException.class, () -> authService.register(request));
        }

        @Test
        @DisplayName("Should reject password without uppercase")
        void register_NoUppercasePassword() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setEmail("new@example.com");
            request.setPassword("password1");

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

            assertThrows(BadRequestException.class, () -> authService.register(request));
        }

        @Test
        @DisplayName("Should reject password without digits")
        void register_NoDigitPassword() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setEmail("new@example.com");
            request.setPassword("Password");

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

            assertThrows(BadRequestException.class, () -> authService.register(request));
        }

        @Test
        @DisplayName("Should reject password with spaces")
        void register_PasswordWithSpaces() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setEmail("new@example.com");
            request.setPassword("Pass word1");

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

            assertThrows(BadRequestException.class, () -> authService.register(request));
        }

        @Test
        @DisplayName("Should reject password with invalid special characters")
        void register_PasswordInvalidSpecialChars() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setEmail("new@example.com");
            request.setPassword("Passw0rd!");

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

            assertThrows(BadRequestException.class, () -> authService.register(request));
        }

        @Test
        @DisplayName("Should accept password with allowed special chars _ - @ .")
        void register_PasswordWithAllowedSpecialChars() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setEmail("new@example.com");
            request.setPassword("Pass_1-@.");

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(roleRepository.findByRoleName(RoleName.GUEST)).thenReturn(Optional.of(guestRole));
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
            when(userMapper.toDto(any(User.class))).thenReturn(null);

            RegisterResponse result = authService.register(request);
            assertNotNull(result);
        }

        @Test
        @DisplayName("Should reject null password")
        void register_NullPassword() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setEmail("new@example.com");
            request.setPassword(null);

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

            assertThrows(BadRequestException.class, () -> authService.register(request));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // REGISTER ADMIN
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Register Admin Tests")
    class RegisterAdminTests {

        @Test
        @DisplayName("Should register admin with STAF_LAPANGAN role")
        void registerAdmin_StafLapangan() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("staf1");
            request.setEmail("staf@example.com");
            request.setPassword("Password1");
            request.setRole("STAF_LAPANGAN");

            Role stafRole = Role.builder().id(3).roleName(RoleName.STAF_LAPANGAN).build();

            when(userRepository.existsByUsername("staf1")).thenReturn(false);
            when(userRepository.existsByEmail("staf@example.com")).thenReturn(false);
            when(roleRepository.findByRoleName(RoleName.STAF_LAPANGAN)).thenReturn(Optional.of(stafRole));
            when(passwordEncoder.encode("Password1")).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
            when(userMapper.toDto(any(User.class))).thenReturn(null);

            RegisterResponse result = authService.registerAdmin(request);
            assertNotNull(result);
        }

        @Test
        @DisplayName("Should set PENDING status for admin registration")
        void registerAdmin_ShouldSetPendingStatus() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("staf1");
            request.setEmail("staf@example.com");
            request.setPassword("Password1");
            request.setRole("STAF_LAPANGAN");

            Role stafRole = Role.builder().id(3).roleName(RoleName.STAF_LAPANGAN).build();

            when(userRepository.existsByUsername("staf1")).thenReturn(false);
            when(userRepository.existsByEmail("staf@example.com")).thenReturn(false);
            when(roleRepository.findByRoleName(RoleName.STAF_LAPANGAN)).thenReturn(Optional.of(stafRole));
            when(passwordEncoder.encode("Password1")).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
            when(userMapper.toDto(any(User.class))).thenReturn(null);

            authService.registerAdmin(request);

            verify(userRepository).save(argThat(
                    user -> user.getStatus() != null && UserStatusName.PENDING.equals(user.getStatus().getName())));
        }

        @Test
        @DisplayName("Should register admin with OWNER role")
        void registerAdmin_Owner() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("owner1");
            request.setEmail("owner@example.com");
            request.setPassword("Password1");
            request.setRole("OWNER");

            Role ownerRole = Role.builder().id(5).roleName(RoleName.OWNER).build();

            when(userRepository.existsByUsername("owner1")).thenReturn(false);
            when(userRepository.existsByEmail("owner@example.com")).thenReturn(false);
            when(roleRepository.findByRoleName(RoleName.OWNER)).thenReturn(Optional.of(ownerRole));
            when(passwordEncoder.encode("Password1")).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
            when(userMapper.toDto(any(User.class))).thenReturn(null);

            RegisterResponse result = authService.registerAdmin(request);
            assertNotNull(result);
        }

        @Test
        @DisplayName("Should reject admin registration with null role")
        void registerAdmin_NullRole() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newadmin");
            request.setEmail("admin@example.com");
            request.setPassword("Password1");
            request.setRole(null);

            when(userRepository.existsByUsername("newadmin")).thenReturn(false);
            when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);

            assertThrows(BadRequestException.class, () -> authService.registerAdmin(request));
        }

        @Test
        @DisplayName("Should reject admin registration with GUEST role")
        void registerAdmin_GuestRoleRejected() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newadmin");
            request.setEmail("admin@example.com");
            request.setPassword("Password1");
            request.setRole("GUEST");

            when(userRepository.existsByUsername("newadmin")).thenReturn(false);
            when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);

            assertThrows(BadRequestException.class, () -> authService.registerAdmin(request));
        }

        @Test
        @DisplayName("Should reject admin registration with unknown role")
        void registerAdmin_UnknownRole() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newadmin");
            request.setEmail("admin@example.com");
            request.setPassword("Password1");
            request.setRole("SUPERUSER");

            when(userRepository.existsByUsername("newadmin")).thenReturn(false);
            when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);

            assertThrows(BadRequestException.class, () -> authService.registerAdmin(request));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LOGOUT
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Logout Tests")
    class LogoutTests {

        @Test
        @DisplayName("Should logout successfully with valid Bearer token")
        void logout_Success() {
            String token = "valid-jwt-token";
            when(jwtUtils.extractUsername(token)).thenReturn("testuser");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            authService.logout("Bearer " + token);

            verify(jwtTokenBlacklist).blacklistToken(token);
            verify(refreshTokenService).deleteRefreshTokenByUsername("testuser");
            verify(userRepository).save(argThat(user -> user.getLastLogoutAt() != null));
        }

        @Test
        @DisplayName("Should handle logout with null header")
        void logout_NullHeader() {
            authService.logout(null);
            verify(jwtTokenBlacklist, never()).blacklistToken(anyString());
        }

        @Test
        @DisplayName("Should handle logout with non-Bearer header")
        void logout_NonBearerHeader() {
            authService.logout("Basic abc123");
            verify(jwtTokenBlacklist, never()).blacklistToken(anyString());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LOGIN ATTEMPT RATE LIMITING
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Login Attempt Rate Limiting Tests")
    class LoginAttemptTests {

        @Test
        @DisplayName("Should throw TooManyRequestException when credential is locked out")
        void login_Locked_ThrowsTooManyRequestException() {
            LoginRequest request = new LoginRequest();
            request.setUsernameOrEmail("testuser");
            request.setPassword("Password1");

            when(loginAttemptService.isLocked("testuser")).thenReturn(true);
            when(loginAttemptService.getLockoutUntil("testuser"))
                    .thenReturn(LocalDateTime.now().plusMinutes(1));

            assertThrows(TooManyRequestException.class, () -> authService.login(request, null));
            verify(authenticationManager, never()).authenticate(any());
        }

        @Test
        @DisplayName("Should record failed attempt on bad credentials")
        void login_BadCredentials_RecordsFailedAttempt() {
            LoginRequest request = new LoginRequest();
            request.setUsernameOrEmail("testuser");
            request.setPassword("wrong");

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThrows(BadCredentialsException.class, () -> authService.login(request, null));
            verify(loginAttemptService).recordFailedAttempt("testuser");
            verify(loginAttemptService, never()).resetAttempts(anyString());
        }

        @Test
        @DisplayName("Should reset attempts on successful login")
        void login_Success_ResetsAttempts() {
            LoginRequest request = new LoginRequest();
            request.setUsernameOrEmail("testuser");
            request.setPassword("Password1");

            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn(userDetails);
            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(jwtUtils.generateAccessToken(userDetails)).thenReturn("token");
            when(refreshTokenService.createRefreshToken("testuser")).thenReturn("refresh");
            when(authMapper.toAuthResponse("token", "refresh", userDetails))
                    .thenReturn(AuthResponse.builder().accessToken("token").build());

            authService.login(request, null);

            verify(loginAttemptService).resetAttempts("testuser");
            verify(loginAttemptService, never()).recordFailedAttempt(anyString());
        }

        @Test
        @DisplayName("Should login successfully using an email address")
        void login_WithEmail_Success() {
            LoginRequest request = new LoginRequest();
            request.setUsernameOrEmail("test@example.com");
            request.setPassword("Password1");

            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn(userDetails);
            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(jwtUtils.generateAccessToken(userDetails)).thenReturn("token");
            when(refreshTokenService.createRefreshToken("testuser")).thenReturn("refresh");
            when(authMapper.toAuthResponse("token", "refresh", userDetails))
                    .thenReturn(AuthResponse.builder().accessToken("token").build());

            AuthResponse result = authService.login(request, null);

            assertNotNull(result);
            verify(loginAttemptService).resetAttempts("test@example.com");
        }
    }

    @Nested
    @DisplayName("Deactivate Account Tests")
    class DeactivateAccountTests {

        @Test
        @DisplayName("Should deactivate account and revoke tokens for valid bearer token")
        void deactivateAccount_Success() {
            String token = "valid-token";
            String authHeader = "Bearer " + token;

            when(jwtTokenBlacklist.isTokenBlacklisted(token)).thenReturn(false);
            when(jwtUtils.extractUsername(token)).thenReturn("testuser");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            org.springframework.security.core.userdetails.UserDetails springUserDetails = new org.springframework.security.core.userdetails.User(
                    "testuser", "pass", List.of(new SimpleGrantedAuthority("GUEST")));

            when(userDetailsService.loadUserByUsername("testuser")).thenReturn(springUserDetails);
            when(jwtUtils.validateToken(token, springUserDetails)).thenReturn(true);
            when(userStatusRepository.findByName(UserStatusName.NON_AKTIF)).thenReturn(Optional.of(nonActiveStatus));

            authService.deactivateAccount(authHeader);

            verify(userRepository, atLeastOnce())
                    .save(argThat(user -> user.getStatus() != null
                            && UserStatusName.NON_AKTIF.equals(user.getStatus().getName())));
            verify(jwtTokenBlacklist).blacklistToken(token);
            verify(refreshTokenService).deleteRefreshTokenByUsername("testuser");
        }

        @Test
        @DisplayName("Should reject deactivate account when authorization header is invalid")
        void deactivateAccount_InvalidHeader() {
            assertThrows(BadRequestException.class, () -> authService.deactivateAccount("invalid-header"));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // REFRESH TOKEN
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Refresh Token Tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should refresh token successfully")
        void refreshToken_Success() {
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("valid-refresh-token");

            when(refreshTokenService.validateRefreshToken("valid-refresh-token")).thenReturn("testuser");

            org.springframework.security.core.userdetails.UserDetails springUserDetails = new org.springframework.security.core.userdetails.User(
                    "testuser", "pass", List.of(new SimpleGrantedAuthority("GUEST")));

            when(userDetailsService.loadUserByUsername("testuser")).thenReturn(springUserDetails);
            when(jwtUtils.generateAccessToken(springUserDetails)).thenReturn("new-access-token");

            AuthResponse expectedResponse = AuthResponse.builder()
                    .accessToken("new-access-token").refreshToken("valid-refresh-token")
                    .username("testuser").role("GUEST").build();
            when(authMapper.toRefreshedAuthResponse("new-access-token", "valid-refresh-token", "testuser", "GUEST"))
                    .thenReturn(expectedResponse);

            AuthResponse result = authService.refreshToken(request);

            assertEquals("new-access-token", result.getAccessToken());
            assertEquals("valid-refresh-token", result.getRefreshToken());
        }
    }
}
