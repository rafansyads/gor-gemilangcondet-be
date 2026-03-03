package io.mpruy.gor_gemilangcondet.backend_api.controller;

import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseResponseDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.users.UserDto;
import io.mpruy.gor_gemilangcondet.backend_api.service.UserService;
import io.mpruy.gor_gemilangcondet.backend_api.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
        try {
            List<UserDto> users = userService.getAllUsers();
            return ResponseUtil.success(users, "Users retrieved successfully", HttpStatus.OK)
                    .toBuilder().build();
        } catch (Exception ex) {
            return ResponseUtil.error(
                    "Failed to retrieve users: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /users/{id}
    // ──────────────────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponseDto<UserDto>> getUserById(@PathVariable UUID id) {
        try {
            UserDto user = userService.getUserById(id);
            return ResponseUtil.success(user, "User retrieved successfully", HttpStatus.OK)
                    .toBuilder().build();
        } catch (IllegalArgumentException ex) {
            return ResponseUtil.error(ex.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            return ResponseUtil.error(
                    "Failed to retrieve user: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /users/by-username/{username}
    // ──────────────────────────────────────────────────────────────────────────

    @GetMapping("/by-username/{username}")
    public ResponseEntity<BaseResponseDto<UserDto>> getUserByUsername(@PathVariable String username) {
        try {
            UserDto user = userService.getUserByUsername(username);
            return ResponseUtil.success(user, "User retrieved successfully", HttpStatus.OK)
                    .toBuilder().build();
        } catch (IllegalArgumentException ex) {
            return ResponseUtil.error(ex.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            return ResponseUtil.error(
                    "Failed to retrieve user: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
