package io.mpruy.gor_gemilangcondet.backend_api.service;

import io.mpruy.gor_gemilangcondet.backend_api.dto.users.UserDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.users.requests.UpdateProfileRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.users.responses.UpdateProfileResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.User;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ConflictException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ResourceNotFoundException;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserRepository;
import io.mpruy.gor_gemilangcondet.backend_api.security.UserDetailsImpl;
import io.mpruy.gor_gemilangcondet.backend_api.security.jwt.JwtUtils;
import io.mpruy.gor_gemilangcondet.backend_api.security.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;

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
                .orElseThrow(() -> new ResourceNotFoundException("Pengguna tidak ditemukan: " + id));
        return toDto(user);
    }

    @Transactional(readOnly = true)
    public UserDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Pengguna tidak ditemukan: " + username));
        return toDto(user);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Update Profile
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Updates the currently authenticated user's username and email.
     * Both fields must remain unique across all users.
     * <p>
     * When the username changes the existing JWT becomes invalid (its subject
     * no longer matches the DB), so this method issues a fresh access token
     * and refresh token keyed to the new username, and updates the
     * SecurityContext so the rest of the request sees the new identity.
     *
     * @param request the new username and email
     * @return updated user DTO together with fresh auth tokens
     */
    @Transactional
    public UpdateProfileResponse updateProfile(UpdateProfileRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User currentUser = userDetails.getUser();
        String oldUsername = currentUser.getUsername();

        // Re-fetch to get a managed entity in the current persistence context
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Pengguna tidak ditemukan"));

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

        // Build new UserDetails with the updated user entity
        UserDetailsImpl newUserDetails = new UserDetailsImpl(updated);

        // Issue fresh tokens with the new username
        String newAccessToken = jwtUtils.generateAccessToken(newUserDetails);

        // Rotate refresh token: revoke old username's token, create one for new username
        refreshTokenService.deleteRefreshTokenByUsername(oldUsername);
        String newRefreshToken = refreshTokenService.createRefreshToken(updated.getUsername());

        // Update SecurityContext so subsequent filters/code in this request see the new principal
        UsernamePasswordAuthenticationToken newAuth =
                new UsernamePasswordAuthenticationToken(newUserDetails, null, newUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(newAuth);

        return UpdateProfileResponse.builder()
                .user(toDto(updated))
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
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
