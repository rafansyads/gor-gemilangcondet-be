package io.mpruy.gor_gemilangcondet.backend_api.service;

import io.mpruy.gor_gemilangcondet.backend_api.dto.users.UserDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.users.requests.UpdateProfileRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.users.responses.UpdateProfileResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.RoleName;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.User;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatusName;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ConflictException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ResourceNotFoundException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.BadRequestException;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserStatusRepository;
import io.mpruy.gor_gemilangcondet.backend_api.security.UserDetailsImpl;
import io.mpruy.gor_gemilangcondet.backend_api.security.jwt.JwtUtils;
import io.mpruy.gor_gemilangcondet.backend_api.security.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserStatusRepository userStatusRepository;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;
    private static final ZoneId ZONE_JAKARTA = ZoneId.of("Asia/Jakarta");

    private static final Set<RoleName> ADMIN_ASSIGNABLE_ROLES = Set.of(
            RoleName.STAF_LAPANGAN,
            RoleName.STAF_TOKO,
            RoleName.OWNER,
            RoleName.ADMIN);

    // ──────────────────────────────────────────────────────────────────────────
    // Queries
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toDto)
                .toList();
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
    // Pending Admin/Staff Registrations (admin review: register-admin)
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<UserDto> getAllAdminUsers() {
        return userRepository.findByRole_RoleNameIn(ADMIN_ASSIGNABLE_ROLES).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllAdminByAdminAssignableStatus() {
        return getAllAdminUsers();
    }

    @Transactional
    public UserDto approvePendingAdminRegistration(UUID id) {
        // The user must be in PENDING or SUSPENDED state
        User pendingUser = findPendingOrSuspendedAdminUser(id);
        pendingUser.setStatus(requireUserStatus(UserStatusName.AKTIF));
        pendingUser.setBannedAt(null);
        User saved = userRepository.save(pendingUser);
        return toDto(saved);
    }

    @Transactional
    public UserDto rejectPendingAdminRegistration(UUID id) {
        // The user must be in PENDING or SUSPENDED state
        User pendingUser = findPendingOrSuspendedAdminUser(id);
        pendingUser.setStatus(requireUserStatus(UserStatusName.BANNED));
        pendingUser.setBannedAt(LocalDateTime.now(ZONE_JAKARTA));
        User saved = userRepository.save(pendingUser);
        return toDto(saved);
    }

    @Transactional
    public UserDto suspendUser(UUID id) {
        // The user must be in AKTIF state
        User user = findActiveAdminUser(id);
        user.setStatus(requireUserStatus(UserStatusName.SUSPENDED));
        User saved = userRepository.save(user);
        return toDto(saved);
    }

    @Transactional
    public int deleteExpiredBannedUsers() {
        LocalDateTime threshold = LocalDateTime.now(ZONE_JAKARTA).minusDays(3);
        List<User> expiredBannedUsers = userRepository.findByStatus_NameAndBannedAtLessThanEqual(
                UserStatusName.BANNED,
                threshold);

        if (expiredBannedUsers.isEmpty()) {
            return 0;
        }

        expiredBannedUsers.forEach(user -> refreshTokenService.deleteRefreshTokenByUsername(user.getUsername()));
        userRepository.deleteAllInBatch(expiredBannedUsers);
        return expiredBannedUsers.size();
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
        if (request.getUsername() == null || request.getEmail() == null) {
            throw new BadRequestException("Username dan email tidak boleh kosong");
        }

        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            throw new BadRequestException("Tidak ada pengguna yang terautentikasi");
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User currentUser = userDetails.getUser();
        String oldUsername = currentUser.getUsername();

        // Re-fetch to get a managed entity in the current persistence context
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Pengguna tidak ditemukan"));

        // Check uniqueness — exclude the current user's own ID
        if (userRepository.existsByUsernameAndIdNot(request.getUsername(), user.getId())) {
            throw new ConflictException("Username sudah digunakan: " + request.getUsername());
        }

        if (userRepository.existsByEmailAndIdNot(request.getEmail(), user.getId())) {
            throw new ConflictException("Email sudah terdaftar: " + request.getEmail());
        }

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        User updated = userRepository.save(user);

        // Build new UserDetails with the updated user entity
        UserDetailsImpl newUserDetails = new UserDetailsImpl(updated);

        // Issue fresh tokens with the new username
        String newAccessToken = jwtUtils.generateAccessToken(newUserDetails);

        // Rotate refresh token: revoke old username's token, create one for new
        // username
        // Rotate refresh token: revoke old username's token, create one for new
        // username
        refreshTokenService.deleteRefreshTokenByUsername(oldUsername);
        String newRefreshToken = refreshTokenService.createRefreshToken(updated.getUsername());

        // Update SecurityContext so subsequent filters/code in this request see the new
        // principal
        UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(newUserDetails, null,
                newUserDetails.getAuthorities());
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
                .status(user.getStatus() != null ? user.getStatus().getName().name() : null)
                .membershipStart(user.getMembershipStart())
                .membershipEnd(user.getMembershipEnd())
                .lastLoginAt(user.getLastLoginAt())
                .lastLogoutAt(user.getLastLogoutAt())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .failedLoginWindowStartedAt(user.getFailedLoginWindowStartedAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .statusChangedAt(user.getStatusChangedAt())
                .bannedAt(user.getBannedAt())
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private User findPendingOrSuspendedAdminUser(UUID id) {
        return userRepository.findByIdAndStatus_NameInAndRole_RoleNameIn(
                id,
                Set.of(UserStatusName.PENDING, UserStatusName.SUSPENDED),
                ADMIN_ASSIGNABLE_ROLES)
                .orElseThrow(() -> new ResourceNotFoundException("Pengguna tidak ditemukan: " + id));
    }

    private User findActiveAdminUser(UUID id) {
        return userRepository.findByIdAndStatus_NameAndRole_RoleNameIn(
                id,
                UserStatusName.AKTIF,
                ADMIN_ASSIGNABLE_ROLES)
                .orElseThrow(() -> new ResourceNotFoundException("Pengguna tidak ditemukan: " + id));
    }

    private UserStatus requireUserStatus(UserStatusName statusName) {
        return userStatusRepository.findByName(statusName)
                .orElseThrow(() -> new IllegalStateException(
                        "Status pengguna tidak ditemukan di database: " + statusName));
    }
}
