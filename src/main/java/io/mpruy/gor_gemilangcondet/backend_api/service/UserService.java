package io.mpruy.gor_gemilangcondet.backend_api.service;

import io.mpruy.gor_gemilangcondet.backend_api.dto.users.UserDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.users.requests.UpdateProfileRequest;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.User;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ConflictException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ResourceNotFoundException;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserRepository;
import io.mpruy.gor_gemilangcondet.backend_api.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // ──────────────────────────────────────────────────────────────────────────
    // Queries
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        return toDto(user);
    }

    @Transactional(readOnly = true)
    public UserDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return toDto(user);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Update Profile
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Updates the currently authenticated user's username and email.
     * Both fields must remain unique across all users.
     *
     * @param request the new username and email
     * @return updated user DTO
     */
    @Transactional
    public UserDto updateProfile(UpdateProfileRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User currentUser = userDetails.getUser();

        // Re-fetch to get a managed entity in the current persistence context
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check uniqueness — exclude the current user's own ID
        if (userRepository.existsByUsernameAndIdNot(request.getUsername(), user.getId())) {
            throw new ConflictException("Username is already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmailAndIdNot(request.getEmail(), user.getId())) {
            throw new ConflictException("Email is already registered: " + request.getEmail());
        }

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        User updated = userRepository.save(user);

        return toDto(updated);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Mapper
    // ──────────────────────────────────────────────────────────────────────────

    private UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().getRoleName())
                .membershipStart(user.getMembershipStart())
                .membershipEnd(user.getMembershipEnd())
                .build();
    }
}
