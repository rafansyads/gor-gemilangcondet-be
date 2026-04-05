package io.mpruy.gor_gemilangcondet.backend_api.service;

import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.requests.LoginRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.requests.RegisterAdminRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.requests.RegisterRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.responses.LoginResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.responses.RegisterResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.roles.Role;
import io.mpruy.gor_gemilangcondet.backend_api.entities.roles.RoleName;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.User;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatusName;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ForbiddenException;
import io.mpruy.gor_gemilangcondet.backend_api.repository.RoleRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserStatusRepository;
import io.mpruy.gor_gemilangcondet.backend_api.security.UserDetailsImpl;
import io.mpruy.gor_gemilangcondet.backend_api.security.jwt.JwtUtils;
import io.mpruy.gor_gemilangcondet.backend_api.service.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks AuthService authService;
    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock UserStatusRepository userStatusRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtUtils jwtUtils;
    @Mock UserMapper userMapper;

    private Role pelangganRole() {
        return Role.builder().id(3L).name(RoleName.ROLE_PELANGGAN).build();
    }

    private Role adminRole() {
        return Role.builder().id(1L).name(RoleName.ROLE_ADMIN).build();
    }

    private UserStatus aktifStatus() {
        return UserStatus.builder().id(2L).name(UserStatusName.AKTIF).build();
    }

    private UserStatus pendingStatus() {
        return UserStatus.builder().id(1L).name(UserStatusName.PENDING).build();
    }

    @Test
    void register_shouldSetAktifStatus() {
        when(userRepository.existsByUsername("user1")).thenReturn(false);
        when(userRepository.existsByEmail("user1@test.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_PELANGGAN)).thenReturn(Optional.of(pelangganRole()));
        when(userStatusRepository.findByName(UserStatusName.AKTIF)).thenReturn(Optional.of(aktifStatus()));
        when(passwordEncoder.encode(any())).thenReturn("encoded");

        User savedUser = User.builder().id("u1").username("user1").email("user1@test.com")
                .password("encoded").role(pelangganRole()).status(aktifStatus()).build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toRegisterResponse(any())).thenReturn(new RegisterResponse());

        authService.register(new RegisterRequest("user1", "user1@test.com", "password123"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus().getName()).isEqualTo(UserStatusName.AKTIF);
    }

    @Test
    void registerAdmin_shouldSetPendingStatus() {
        when(userRepository.existsByUsername("admin2")).thenReturn(false);
        when(userRepository.existsByEmail("admin2@test.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_ADMIN)).thenReturn(Optional.of(adminRole()));
        when(userStatusRepository.findByName(UserStatusName.PENDING)).thenReturn(Optional.of(pendingStatus()));
        when(passwordEncoder.encode(any())).thenReturn("encoded");

        User savedUser = User.builder().id("a2").username("admin2").email("admin2@test.com")
                .password("encoded").role(adminRole()).status(pendingStatus()).build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toRegisterResponse(any())).thenReturn(new RegisterResponse());

        authService.registerAdmin(new RegisterAdminRequest("admin2", "admin2@test.com", "password123", RoleName.ROLE_ADMIN));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus().getName()).isEqualTo(UserStatusName.PENDING);
    }

    @Test
    void registerAdmin_shouldThrowForbiddenException_whenRoleIsPelanggan() {
        assertThatThrownBy(() ->
                authService.registerAdmin(new RegisterAdminRequest("u", "u@test.com", "password123", RoleName.ROLE_PELANGGAN)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void login_shouldThrowForbiddenException_whenUserIsPending() {
        UserDetailsImpl pendingUser = new UserDetailsImpl("id", "admin2", "admin2@test.com",
                "encoded", List.of(), UserStatusName.PENDING);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(pendingUser);
        when(authenticationManager.authenticate(any())).thenReturn(auth);

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin2", "password123")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    void login_shouldReturnToken_whenUserIsAktif() {
        UserDetailsImpl aktifUser = new UserDetailsImpl("id", "admin", "admin@test.com",
                "encoded", List.of(), UserStatusName.AKTIF);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(aktifUser);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtUtils.generateJwtToken(auth)).thenReturn("test.jwt.token");

        User user = User.builder().id("id").username("admin").email("admin@test.com")
                .password("encoded").role(adminRole()).status(aktifStatus()).build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        LoginResponse response = authService.login(new LoginRequest("admin", "password123"));

        assertThat(response.getToken()).isEqualTo("test.jwt.token");
    }
}
