package io.mpruy.gor_gemilangcondet.backend_api.controller;

import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseRequestDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseResponseDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.users.UserDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.users.requests.UpdateProfileRequest;
import io.mpruy.gor_gemilangcondet.backend_api.service.UserService;
import io.mpruy.gor_gemilangcondet.backend_api.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    // PUT /users/profile
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Update the currently authenticated user's username and email.
     * Both must remain unique across all users.
     *
     * @param request wrapped {@link UpdateProfileRequest}
     */
    @PutMapping("/profile")
    public ResponseEntity<BaseResponseDto<UserDto>> updateProfile(
            @Validated @RequestBody BaseRequestDto<UpdateProfileRequest> request) {
        UserDto updatedUser = userService.updateProfile(request.getData());
        return ResponseUtil.success(updatedUser, "Profil berhasil diperbarui", HttpStatus.OK)
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
    public ResponseEntity<BaseResponseDto<UserDto>> updateProfile(
            @Validated @RequestBody BaseRequestDto<UpdateProfileRequest> request) {
        try {
            UserDto updatedUser = userService.updateProfile(request.getData());
            return ResponseUtil.success(updatedUser, "Profile updated successfully", HttpStatus.OK)
                    .toBuilder().build();
        } catch (IllegalArgumentException ex) {
            return ResponseUtil.error(ex.getMessage(), HttpStatus.CONFLICT);
        } catch (Exception ex) {
            return ResponseUtil.error(
                    "Failed to update profile: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
