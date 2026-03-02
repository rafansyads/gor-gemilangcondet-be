package io.mpruy.gor_gemilangcondet.backend_api.controller;

import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseRequestDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseResponseDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.requests.LoginRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.requests.RefreshTokenRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.requests.RegisterRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.responses.AuthResponse;
import io.mpruy.gor_gemilangcondet.backend_api.service.AuthService;
import io.mpruy.gor_gemilangcondet.backend_api.util.ResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
        try {
            AuthResponse authResponse = authService.login(request.getData(), redirectUrl);
            return ResponseUtil.success(authResponse, "Login successful", HttpStatus.OK)
                    .toBuilder().build();
        } catch (Exception ex) {
            return ResponseUtil.error(
                    "Login failed: " + ex.getMessage(),
                    HttpStatus.UNAUTHORIZED);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /auth/register  (public — always GUEST)
    // ──────────────────────────────────────────────────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<BaseResponseDto<AuthResponse>> register(
            @Validated @RequestBody BaseRequestDto<RegisterRequest> request) {
        try {
            AuthResponse authResponse = authService.register(request.getData());
            return ResponseUtil.success(authResponse, "Registration successful", HttpStatus.CREATED)
                    .toBuilder().build();
        } catch (IllegalArgumentException ex) {
            return ResponseUtil.error(ex.getMessage(), HttpStatus.CONFLICT);
        } catch (Exception ex) {
            return ResponseUtil.error(
                    "Registration failed: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /auth/register-admin  (privileged — STAF_LAPANGAN/STAF_TOKO/OWNER/ADMIN)
    // ──────────────────────────────────────────────────────────────────────────

    @PostMapping("/register-admin")
    public ResponseEntity<BaseResponseDto<AuthResponse>> registerAdmin(
            @Validated @RequestBody BaseRequestDto<RegisterRequest> request) {
        try {
            AuthResponse authResponse = authService.registerAdmin(request.getData());
            return ResponseUtil.success(
                    authResponse,
                    "Admin/staff account created successfully",
                    HttpStatus.CREATED)
                    .toBuilder().build();
        } catch (IllegalArgumentException ex) {
            return ResponseUtil.error(ex.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception ex) {
            return ResponseUtil.error(
                    "Admin registration failed: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /auth/logout
    // ──────────────────────────────────────────────────────────────────────────

    @PostMapping("/logout")
    public ResponseEntity<BaseResponseDto<String>> logout(HttpServletRequest httpRequest) {
        try {
            String authHeader = httpRequest.getHeader("Authorization");
            authService.logout(authHeader);
            return ResponseUtil.success("Logged out successfully", "Logout successful", HttpStatus.OK)
                    .toBuilder().build();
        } catch (Exception ex) {
            return ResponseUtil.error(
                    "Logout failed: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /auth/refresh
    // ──────────────────────────────────────────────────────────────────────────

    @PostMapping("/refresh")
    public ResponseEntity<BaseResponseDto<AuthResponse>> refresh(
            @Validated @RequestBody BaseRequestDto<RefreshTokenRequest> request) {
        try {
            AuthResponse authResponse = authService.refreshToken(request.getData());
            return ResponseUtil.success(authResponse, "Token refreshed", HttpStatus.OK)
                    .toBuilder().build();
        } catch (IllegalArgumentException ex) {
            return ResponseUtil.error(ex.getMessage(), HttpStatus.UNAUTHORIZED);
        } catch (Exception ex) {
            return ResponseUtil.error(
                    "Token refresh failed: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
