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
    public ResponseEntity<BaseResponseDto<List<UserDto>>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers();
        return ResponseUtil.success(users, "Daftar pengguna berhasil diambil", HttpStatus.OK)
                .toBuilder().build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /users/{id}
    // ──────────────────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponseDto<UserDto>> getUserById(@PathVariable UUID id) {
        UserDto user = userService.getUserById(id);
        return ResponseUtil.success(user, "Pengguna berhasil diambil", HttpStatus.OK)
                .toBuilder().build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /users/by-username/{username}
    // ──────────────────────────────────────────────────────────────────────────

    @GetMapping("/by-username/{username}")
    public ResponseEntity<BaseResponseDto<UserDto>> getUserByUsername(@PathVariable String username) {
        UserDto user = userService.getUserByUsername(username);
        return ResponseUtil.success(user, "Pengguna berhasil diambil", HttpStatus.OK)
                .toBuilder().build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /users/pending-admin-registrations
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Lists pending admin/staff registrations.
     * Only ADMIN can review these submissions.
     */
    @GetMapping("/pending-admin-registrations")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BaseResponseDto<List<UserDto>>> getPendingAdminRegistrations() {
        List<UserDto> pendingUsers = userService.getPendingAdminRegistrations();
        return ResponseUtil
                .success(pendingUsers, "Daftar registrasi admin/staff pending berhasil diambil", HttpStatus.OK)
                .toBuilder().build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PATCH /users/pending-admin-registrations/{id}/approve
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Approves a pending admin/staff registration (PENDING -> AKTIF).
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
     * Rejects a pending admin/staff registration by deleting the pending user.
     * Only ADMIN can perform this action.
     */
    @DeleteMapping("/pending-admin-registrations/{id}/reject")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BaseResponseDto<String>> rejectPendingAdminRegistration(@PathVariable UUID id) {
        userService.rejectPendingAdminRegistration(id);
        return ResponseUtil
                .success("Registrasi pending ditolak", "Registrasi admin/staff berhasil ditolak", HttpStatus.OK)
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
