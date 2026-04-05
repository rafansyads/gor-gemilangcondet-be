package io.mpruy.gor_gemilangcondet.backend_api.service;

import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.requests.LoginRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.requests.RegisterAdminRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.requests.RegisterRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.responses.LoginResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.responses.RegisterResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.roles.RoleName;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.User;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatusName;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ForbiddenException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.UnauthorizedException;
import io.mpruy.gor_gemilangcondet.backend_api.repository.RoleRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserStatusRepository;
import io.mpruy.gor_gemilangcondet.backend_api.security.UserDetailsImpl;
import io.mpruy.gor_gemilangcondet.backend_api.security.jwt.JwtUtils;
import io.mpruy.gor_gemilangcondet.backend_api.service.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserStatusRepository userStatusRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private UserMapper userMapper;

    public RegisterResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new RuntimeException("Username sudah digunakan");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email sudah digunakan");
        }
        var role = roleRepository.findByName(RoleName.ROLE_PELANGGAN)
                .orElseThrow(() -> new RuntimeException("Role PELANGGAN tidak ditemukan"));
        var status = userStatusRepository.findByName(UserStatusName.AKTIF)
                .orElseThrow(() -> new RuntimeException("Status AKTIF tidak ditemukan"));

        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(role)
                .status(status)
                .build();
        user = userRepository.save(user);
        return userMapper.toRegisterResponse(user);
    }

    public RegisterResponse registerAdmin(RegisterAdminRequest req) {
        if (req.getRole() == RoleName.ROLE_PELANGGAN) {
            throw new ForbiddenException("Tidak dapat mendaftarkan role PELANGGAN melalui endpoint ini");
        }
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new RuntimeException("Username sudah digunakan");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email sudah digunakan");
        }
        var role = roleRepository.findByName(req.getRole())
                .orElseThrow(() -> new RuntimeException("Role tidak ditemukan: " + req.getRole()));
        var status = userStatusRepository.findByName(UserStatusName.PENDING)
                .orElseThrow(() -> new RuntimeException("Status PENDING tidak ditemukan"));

        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(role)
                .status(status)
                .build();
        user = userRepository.save(user);
        return userMapper.toRegisterResponse(user);
    }

    public LoginResponse login(LoginRequest req) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
        } catch (AuthenticationException e) {
            throw new UnauthorizedException("Username atau password salah");
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        UserStatusName status = userDetails.getStatus();
        if (status != UserStatusName.AKTIF) {
            throw new ForbiddenException("Akun tidak aktif. Status: " + status);
        }

        String token = jwtUtils.generateJwtToken(authentication);
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

        return LoginResponse.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().getName().name())
                .status(user.getStatus().getName().name())
                .build();
    }
}
