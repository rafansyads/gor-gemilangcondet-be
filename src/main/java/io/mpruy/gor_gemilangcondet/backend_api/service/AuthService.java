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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserStatusRepository userStatusRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       UserStatusRepository userStatusRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtUtils jwtUtils,
                       UserMapper userMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userStatusRepository = userStatusRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userMapper = userMapper;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        validateUniqueFields(request);

        Role role = roleRepository.findByName(RoleName.ROLE_PELANGGAN)
                .orElseThrow(() -> new IllegalStateException("Role ROLE_PELANGGAN tidak ditemukan"));

        UserStatus status = userStatusRepository.findByName(UserStatusName.AKTIF)
                .orElseThrow(() -> new IllegalStateException("Status AKTIF tidak ditemukan"));

        User user = User.builder()
                .nama(request.getNama())
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .status(status)
                .build();

        User savedUser = userRepository.save(user);
        return userMapper.toRegisterResponse(savedUser);
    }

    @Transactional
    public RegisterResponse registerAdmin(RegisterRequest request) {
        validateUniqueFields(request);

        Role role = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("Role ROLE_ADMIN tidak ditemukan"));

        UserStatus status = userStatusRepository.findByName(UserStatusName.PENDING)
                .orElseThrow(() -> new IllegalStateException("Status PENDING tidak ditemukan"));

        User user = User.builder()
                .nama(request.getNama())
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .status(status)
                .build();

        User savedUser = userRepository.save(user);
        return userMapper.toRegisterResponse(savedUser);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Username atau password salah"));

        checkUserStatus(user);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            String jwt = jwtUtils.generateJwtToken(authentication);
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            String role = userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(auth -> auth.getAuthority())
                    .orElse("");

            return LoginResponse.builder()
                    .token(jwt)
                    .type("Bearer")
                    .id(userDetails.getId())
                    .username(userDetails.getUsername())
                    .email(userDetails.getEmail())
                    .role(role)
                    .status(user.getStatus().getName().name())
                    .build();
        } catch (BadCredentialsException e) {
            throw new UnauthorizedException("Username atau password salah");
        }
    }

    private void checkUserStatus(User user) {
        UserStatusName statusName = user.getStatus().getName();
        switch (statusName) {
            case PENDING -> throw new ForbiddenException(
                    "Akun Anda sedang menunggu persetujuan admin");
            case NON_AKTIF -> throw new ForbiddenException(
                    "Akun Anda tidak aktif");
            case SUSPENDED -> throw new ForbiddenException(
                    "Akun Anda sedang ditangguhkan");
            case BANNED -> throw new ForbiddenException(
                    "Akun Anda telah diblokir");
            default -> {
            }
        }
    }

    private void validateUniqueFields(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username sudah digunakan");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email sudah digunakan");
        }
    }
}
