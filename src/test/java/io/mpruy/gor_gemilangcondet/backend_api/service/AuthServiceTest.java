package io.mpruy.gor_gemilangcondet.backend_api.service;

import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.requests.LoginRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.requests.RegisterRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.responses.LoginResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.responses.RegisterResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.*;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ForbiddenException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.UnauthorizedException;
import io.mpruy.gor_gemilangcondet.backend_api.repository.RoleRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserStatusRepository;
import io.mpruy.gor_gemilangcondet.backend_api.security.UserDetailsImpl;
import io.mpruy.gor_gemilangcondet.backend_api.security.jwt.JwtUtils;
import io.mpruy.gor_gemilangcondet.backend_api.service.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserStatusRepository userStatusRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    private Role pelangganRole;
    private Role adminRole;
    private UserStatus aktifStatus;
    private UserStatus pendingStatus;

    @BeforeEach
    void setUp() {
        pelangganRole = Role.builder().id(1L).name(RoleName.ROLE_PELANGGAN).build();
        adminRole = Role.builder().id(2L).name(RoleName.ROLE_ADMIN).build();
        aktifStatus = UserStatus.builder().id(1L).name(UserStatusName.AKTIF).build();
        pendingStatus = UserStatus.builder().id(2L).name(UserStatusName.PENDING).build();
    }

    @Test
    void register_shouldAssignAktifStatus() {
        RegisterRequest request = new RegisterRequest("Budi", "budi@test.com", "budi", "pass123");

        when(userRepository.existsByUsername("budi")).thenReturn(false);
        when(userRepository.existsByEmail("budi@test.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_PELANGGAN)).thenReturn(Optional.of(pelangganRole));
        when(userStatusRepository.findByName(UserStatusName.AKTIF)).thenReturn(Optional.of(aktifStatus));
        when(passwordEncoder.encode("pass123")).thenReturn("encodedPass");

        User savedUser = User.builder()
                .id(1L).nama("Budi").email("budi@test.com").username("budi")
                .password("encodedPass").role(pelangganRole).status(aktifStatus).build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        RegisterResponse expectedResponse = RegisterResponse.builder()
                .id(1L).username("budi").status(UserStatusName.AKTIF.name()).build();
        when(userMapper.toRegisterResponse(savedUser)).thenReturn(expectedResponse);

        RegisterResponse response = authService.register(request);

        assertThat(response.getStatus()).isEqualTo(UserStatusName.AKTIF.name());
        verify(userStatusRepository).findByName(UserStatusName.AKTIF);
    }

    @Test
    void registerAdmin_shouldAssignPendingStatus() {
        RegisterRequest request = new RegisterRequest("Admin Baru", "admin@test.com", "adminbaru", "pass123");

        when(userRepository.existsByUsername("adminbaru")).thenReturn(false);
        when(userRepository.existsByEmail("admin@test.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
        when(userStatusRepository.findByName(UserStatusName.PENDING)).thenReturn(Optional.of(pendingStatus));
        when(passwordEncoder.encode("pass123")).thenReturn("encodedPass");

        User savedUser = User.builder()
                .id(2L).nama("Admin Baru").email("admin@test.com").username("adminbaru")
                .password("encodedPass").role(adminRole).status(pendingStatus).build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        RegisterResponse expectedResponse = RegisterResponse.builder()
                .id(2L).username("adminbaru").status(UserStatusName.PENDING.name()).build();
        when(userMapper.toRegisterResponse(savedUser)).thenReturn(expectedResponse);

        RegisterResponse response = authService.registerAdmin(request);

        assertThat(response.getStatus()).isEqualTo(UserStatusName.PENDING.name());
        verify(userStatusRepository).findByName(UserStatusName.PENDING);
        verify(roleRepository).findByName(RoleName.ROLE_ADMIN);
    }

    @Test
    void register_shouldThrowException_whenUsernameExists() {
        RegisterRequest request = new RegisterRequest("Budi", "budi@test.com", "budi", "pass123");
        when(userRepository.existsByUsername("budi")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username sudah digunakan");
    }

    @Test
    void register_shouldThrowException_whenEmailExists() {
        RegisterRequest request = new RegisterRequest("Budi", "budi@test.com", "budi", "pass123");
        when(userRepository.existsByUsername("budi")).thenReturn(false);
        when(userRepository.existsByEmail("budi@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email sudah digunakan");
    }

    @Test
    void login_shouldThrowForbiddenException_whenUserIsPending() {
        LoginRequest request = new LoginRequest("adminbaru", "pass123");

        User pendingUser = User.builder()
                .id(2L).username("adminbaru").password("encodedPass")
                .role(adminRole).status(pendingStatus).build();
        when(userRepository.findByUsername("adminbaru")).thenReturn(Optional.of(pendingUser));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("menunggu persetujuan admin");
    }

    @Test
    void login_shouldThrowForbiddenException_whenUserIsSuspended() {
        UserStatus suspendedStatus = UserStatus.builder().id(3L).name(UserStatusName.SUSPENDED).build();
        LoginRequest request = new LoginRequest("user1", "pass123");

        User suspendedUser = User.builder()
                .id(3L).username("user1").password("encodedPass")
                .role(pelangganRole).status(suspendedStatus).build();
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(suspendedUser));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("ditangguhkan");
    }

    @Test
    void login_shouldThrowForbiddenException_whenUserIsBanned() {
        UserStatus bannedStatus = UserStatus.builder().id(4L).name(UserStatusName.BANNED).build();
        LoginRequest request = new LoginRequest("user2", "pass123");

        User bannedUser = User.builder()
                .id(4L).username("user2").password("encodedPass")
                .role(pelangganRole).status(bannedStatus).build();
        when(userRepository.findByUsername("user2")).thenReturn(Optional.of(bannedUser));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("diblokir");
    }

    @Test
    void login_shouldThrowUnauthorizedException_whenUserNotFound() {
        LoginRequest request = new LoginRequest("notexist", "pass123");
        when(userRepository.findByUsername("notexist")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Username atau password salah");
    }

    @Test
    void login_shouldThrowUnauthorizedException_whenBadCredentials() {
        LoginRequest request = new LoginRequest("budi", "wrongpass");

        User aktifUser = User.builder()
                .id(1L).username("budi").password("encodedPass")
                .role(pelangganRole).status(aktifStatus).build();
        when(userRepository.findByUsername("budi")).thenReturn(Optional.of(aktifUser));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Username atau password salah");
    }

    @Test
    void login_shouldReturnLoginResponse_whenCredentialsAreValid() {
        LoginRequest request = new LoginRequest("budi", "pass123");

        User aktifUser = User.builder()
                .id(1L).username("budi").email("budi@test.com").password("encodedPass")
                .role(pelangganRole).status(aktifStatus).build();
        when(userRepository.findByUsername("budi")).thenReturn(Optional.of(aktifUser));

        UserDetailsImpl userDetails = new UserDetailsImpl(
                1L, "budi", "encodedPass", "budi@test.com", UserStatusName.AKTIF,
                List.of(new SimpleGrantedAuthority("ROLE_PELANGGAN")));

        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtUtils.generateJwtToken(auth)).thenReturn("mock.jwt.token");

        LoginResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getType()).isEqualTo("Bearer");
        assertThat(response.getUsername()).isEqualTo("budi");
        assertThat(response.getStatus()).isEqualTo(UserStatusName.AKTIF.name());
    }
}
