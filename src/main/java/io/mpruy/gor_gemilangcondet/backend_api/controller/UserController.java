package io.mpruy.gor_gemilangcondet.backend_api.controller;

import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseRequestDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseResponseDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.users.UserDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.users.requests.UpdateProfileRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.users.responses.UpdateProfileResponse;
import io.mpruy.gor_gemilangcondet.backend_api.service.UserService;
import io.mpruy.gor_gemilangcondet.backend_api.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ──────────────────────────────────────────────────────────────────────────
    // GET /users
    // ──────────────────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')") // Only ADMIN can list all users
    public ResponseEntity<BaseResponseDto<List<UserDto>>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers();
        return ResponseUtil.success(users, "Daftar pengguna berhasil diambil", HttpStatus.OK)
                .toBuilder().build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /users/{id}
    // ──────────────────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN') or #id == authentication.principal.id")
    // ADMIN can access any user; users can access their own data
    public ResponseEntity<BaseResponseDto<UserDto>> getUserById(@PathVariable UUID id) {
        UserDto user = userService.getUserById(id);
        return ResponseUtil.success(user, "Pengguna berhasil diambil", HttpStatus.OK)
                .toBuilder().build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /users/by-username/{username}
    // ──────────────────────────────────────────────────────────────────────────

    @GetMapping("/by-username/{username}")
    @PreAuthorize("hasAuthority('ADMIN') or #username == authentication.principal.username")
    // ADMIN can access any user; users can access their own data
    public ResponseEntity<BaseResponseDto<UserDto>> getUserByUsername(@PathVariable String username) {
        UserDto user = userService.getUserByUsername(username);
        return ResponseUtil.success(user, "Pengguna berhasil diambil", HttpStatus.OK)
                .toBuilder().build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /users/pending-admin-registrations
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Lists all the admin/staff users, regardless the status.
     * Only ADMIN can review these submissions.
     */
    @GetMapping("/admin-users")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BaseResponseDto<List<UserDto>>> getAllAdminUsers() {
        List<UserDto> adminUsers = userService.getAllAdminUsers();
        return ResponseUtil
                .success(adminUsers, "Daftar pengguna admin/staff berhasil diambil", HttpStatus.OK)
                .toBuilder().build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PATCH /users/pending-admin-registrations/{id}/approve
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Approves a pending/suspended admin/staff registration (PENDING/SUSPENDED ->
     * AKTIF).
     * Only ADMIN can perform this action.
     */
    @PatchMapping("/pending-admin-registrations/{id}/approve")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BaseResponseDto<UserDto>> approvePendingAdminRegistration(@PathVariable UUID id) {
        UserDto approved = userService.approvePendingAdminRegistration(id);
        return ResponseUtil.success(approved, "Registrasi admin/staff berhasil disetujui", HttpStatus.OK)
                .toBuilder().build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // DELETE /users/pending-admin-registrations/{id}/reject
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Rejects a pending/suspended admin/staff registration (PENDING/SUSPENDED ->
     * BANNED). The banned user will be deleted from the database 3 days after the
     * rejection.
     * Only ADMIN can perform this action.
     */
    @DeleteMapping("/pending-admin-registrations/{id}/reject")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BaseResponseDto<UserDto>> rejectPendingAdminRegistration(@PathVariable UUID id) {
        UserDto rejected = userService.rejectPendingAdminRegistration(id);
        return ResponseUtil
                .success(rejected, "Registrasi admin/staff berhasil ditolak", HttpStatus.OK)
                .toBuilder().build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PATCH /users/{id}/suspend
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Suspend a user account (AKTIF -> SUSPENDED). Suspended users cannot log in
     * until their account is reactivated by an admin.
     * Only ADMIN can perform this action.
     */
    @PatchMapping("/{id}/suspend")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BaseResponseDto<UserDto>> suspendUser(@PathVariable UUID id) {
        UserDto suspended = userService.suspendUser(id);
        return ResponseUtil.success(suspended, "Pengguna berhasil disuspend", HttpStatus.OK)
                .toBuilder().build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PUT /users/profile
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Update the currently authenticated user's username and email.
     * Both must remain unique across all users.
     *
     * @param request wrapped {@link UpdateProfileRequest}
     */
    @PutMapping("/profile")
    public ResponseEntity<BaseResponseDto<UpdateProfileResponse>> updateProfile(
            @Validated @RequestBody BaseRequestDto<UpdateProfileRequest> request) {
        UpdateProfileResponse result = userService.updateProfile(request.getData());
        return ResponseUtil.success(result, "Profil berhasil diperbarui", HttpStatus.OK)
                .toBuilder().build();
    }
}
