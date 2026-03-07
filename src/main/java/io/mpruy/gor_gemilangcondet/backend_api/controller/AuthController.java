package io.mpruy.gor_gemilangcondet.backend_api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseRequestDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseResponseDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.requests.LoginRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.requests.RefreshTokenRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.requests.RegisterRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.responses.AuthResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.responses.RegisterResponse;
import io.mpruy.gor_gemilangcondet.backend_api.service.AuthService;
import io.mpruy.gor_gemilangcondet.backend_api.util.ResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ──────────────────────────────────────────────────────────────────────────
    // POST /auth/login
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Authenticate with username + password.
     *
     * @param request     wrapped {@link LoginRequest}
     * @param redirectUrl optional frontend URL; if provided it is echoed in the
     *                    {@link AuthResponse#getRedirectUrl()} field so the client
     *                    knows where to navigate after a successful login
     */
    @PostMapping("/login")
    public ResponseEntity<BaseResponseDto<AuthResponse>> login(
            @Validated @RequestBody BaseRequestDto<LoginRequest> request,
            @RequestParam(name = "redirect", required = false) String redirectUrl) {
        AuthResponse authResponse = authService.login(request.getData(), redirectUrl);
        return ResponseUtil.success(authResponse, "Login successful", HttpStatus.OK)
                .toBuilder().build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /auth/register  (public — always GUEST)
    // ──────────────────────────────────────────────────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<BaseResponseDto<RegisterResponse>> register(
            @Validated @RequestBody BaseRequestDto<RegisterRequest> request) {
        RegisterResponse registerResponse = authService.register(request.getData());
        return ResponseUtil.success(registerResponse, "Registration successful", HttpStatus.CREATED)
                .toBuilder().build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /auth/register-admin  (privileged — STAF_LAPANGAN/STAF_TOKO/OWNER/ADMIN)
    // ──────────────────────────────────────────────────────────────────────────

    @PostMapping("/register-admin")
    public ResponseEntity<BaseResponseDto<RegisterResponse>> registerAdmin(
            @Validated @RequestBody BaseRequestDto<RegisterRequest> request) {
        RegisterResponse registerResponse = authService.registerAdmin(request.getData());
        return ResponseUtil.success(
                registerResponse,
                "Admin/staff account created successfully",
                HttpStatus.CREATED)
                .toBuilder().build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /auth/logout
    // ──────────────────────────────────────────────────────────────────────────

    @PostMapping("/logout")
    public ResponseEntity<BaseResponseDto<String>> logout(HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader("Authorization");
        authService.logout(authHeader);
        return ResponseUtil.success("Logged out successfully", "Logout successful", HttpStatus.OK)
                .toBuilder().build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /auth/refresh
    // ──────────────────────────────────────────────────────────────────────────

    @PostMapping("/refresh")
    public ResponseEntity<BaseResponseDto<AuthResponse>> refresh(
            @Validated @RequestBody BaseRequestDto<RefreshTokenRequest> request) {
        AuthResponse authResponse = authService.refreshToken(request.getData());
        return ResponseUtil.success(authResponse, "Token refreshed", HttpStatus.OK)
                .toBuilder().build();
    }
}
