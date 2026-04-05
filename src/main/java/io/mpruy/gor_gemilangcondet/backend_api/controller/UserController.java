package io.mpruy.gor_gemilangcondet.backend_api.controller;

import io.mpruy.gor_gemilangcondet.backend_api.dto.users.UserDto;
import io.mpruy.gor_gemilangcondet.backend_api.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/pending")
    public ResponseEntity<List<UserDto>> listPendingAdminUsers() {
        return ResponseEntity.ok(userService.listPendingAdminUsers());
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<UserDto> approveUser(@PathVariable String id) {
        return ResponseEntity.ok(userService.approveUser(id));
    }

    @DeleteMapping("/{id}/reject")
    public ResponseEntity<Void> rejectUser(@PathVariable String id) {
        userService.rejectUser(id);
        return ResponseEntity.noContent().build();
    }
}
