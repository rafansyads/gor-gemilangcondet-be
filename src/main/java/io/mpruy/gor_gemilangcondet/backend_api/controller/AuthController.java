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
        return ResponseUtil.success(authResponse, "Berhasil login", HttpStatus.OK)
                .toBuilder().build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /auth/login-admin
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Authenticate with username + password, but only for admin/staff accounts.
     * Used for admin panel login where we want to prevent non-admin users from even
     * attempting to log in.
     * 
     * @param request     wrapped {@link LoginRequest}
     * @param redirectUrl optional frontend URL; if provided it is echoed in the
     *                    {@link AuthResponse#getRedirectUrl()} field so the client
     *                    knows where to navigate after a successful login
     * @return 200 OK with {@link AuthResponse} if credentials are valid and user
     *         has admin/staff role; 403 Forbidden if credentials are valid but user
     *         does not have admin/staff role; 401 Unauthorized if credentials are
     *         invalid
     */
    @PostMapping("/login-admin")
    public ResponseEntity<BaseResponseDto<AuthResponse>> loginAdmin(
            @Validated @RequestBody BaseRequestDto<LoginRequest> request,
            @RequestParam(name = "redirect", required = false) String redirectUrl) {
        AuthResponse authResponse = authService.loginAdmin(request.getData(), redirectUrl);
        return ResponseUtil.success(authResponse, "Login admin berhasil", HttpStatus.OK)
                .toBuilder().build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /auth/register (public — always GUEST)
    // ──────────────────────────────────────────────────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<BaseResponseDto<RegisterResponse>> register(
            @Validated @RequestBody BaseRequestDto<RegisterRequest> request) {
        RegisterResponse registerResponse = authService.register(request.getData());
        return ResponseUtil.success(registerResponse, "Registrasi berhasil", HttpStatus.CREATED)
                .toBuilder().build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /auth/register-admin (privileged — STAF_LAPANGAN/STAF_TOKO/OWNER/ADMIN)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Registers admin/staff account candidates.
     * New users are saved with status PENDING and must be approved by ADMIN.
     * 
     * @param request wrapped {@link RegisterRequest}
     * @return 201 Created with {@link RegisterResponse} if registration
     *         is successful;
     * @return 400 Bad Request if the request data is invalid or if the
     *         registration fails due to business rules (e.g. username/email already
     *         taken)
     */
    @PostMapping("/register-admin")
    public ResponseEntity<BaseResponseDto<RegisterResponse>> registerAdmin(
            @Validated @RequestBody BaseRequestDto<RegisterRequest> request) {
        RegisterResponse registerResponse = authService.registerAdmin(request.getData());
        return ResponseUtil.success(
                registerResponse,
                "Akun admin/staff berhasil diajukan dan menunggu persetujuan admin",
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
        return ResponseUtil.success("Logout berhasil", "Logout berhasil", HttpStatus.OK)
                .toBuilder().build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /auth/refresh
    // ──────────────────────────────────────────────────────────────────────────

    @PostMapping("/refresh")
    public ResponseEntity<BaseResponseDto<AuthResponse>> refresh(
            @Validated @RequestBody BaseRequestDto<RefreshTokenRequest> request) {
        AuthResponse authResponse = authService.refreshToken(request.getData());
        return ResponseUtil.success(authResponse, "Token berhasil diperbarui", HttpStatus.OK)
                .toBuilder().build();
    }
}
