package io.mpruy.gor_gemilangcondet.backend_api.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.requests.LoginRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.requests.RefreshTokenRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.requests.RegisterRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.responses.AuthResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.responses.RegisterResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.Role;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.RoleName;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.User;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatusName;
import io.mpruy.gor_gemilangcondet.backend_api.exception.BadRequestException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ConflictException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ForbiddenException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.TooManyRequestException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.UnauthorizedException;
import io.mpruy.gor_gemilangcondet.backend_api.repository.RoleRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserStatusRepository;
import io.mpruy.gor_gemilangcondet.backend_api.security.UserDetailsImpl;
import io.mpruy.gor_gemilangcondet.backend_api.security.jwt.JwtUtils;
import io.mpruy.gor_gemilangcondet.backend_api.security.service.JwtTokenBlacklist;
import io.mpruy.gor_gemilangcondet.backend_api.security.service.RefreshTokenService;
import io.mpruy.gor_gemilangcondet.backend_api.service.mapper.AuthMapper;
import io.mpruy.gor_gemilangcondet.backend_api.service.mapper.UserMapper;

import java.time.Duration;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final ZoneId ZONE_JAKARTA = ZoneId.of("Asia/Jakarta");

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final JwtTokenBlacklist jwtTokenBlacklist;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsService userDetailsService;
    private final AuthMapper authMapper;
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserStatusRepository userStatusRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;

    /**
     * Roles that only privileged actors (admin portal) may assign during
     * registration.
     */
    private static final Set<RoleName> ADMIN_ASSIGNABLE_ROLES = Set.of(
            RoleName.STAF_LAPANGAN,
            RoleName.STAF_TOKO,
            RoleName.OWNER,
            RoleName.ADMIN);

    /**
     * User status required for successful login. If a user exists with the given
     * credential but does not have this status, login is denied with a 403
     * Forbidden
     */
    private static final UserStatusName ACTIVE_STATUS = UserStatusName.AKTIF;

    // ──────────────────────────────────────────────────────────────────────────
    // Login
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Authenticates the user and issues access + refresh tokens.
     *
     * @param request     username + password
     * @param redirectUrl optional frontend URL to echo back in the response
     * @return {@link AuthResponse} carrying both tokens and the redirect URL
     */
    @Transactional
    public AuthResponse login(LoginRequest request, String redirectUrl) {
        String credential = request.getUsernameOrEmail();
        LocalDateTime now = LocalDateTime.now(ZONE_JAKARTA);
        Optional<User> trackedUserOpt = findUserByCredential(credential);

        trackedUserOpt.ifPresent(user -> {
            if (user.resetFailedLoginAttemptsIfWindowExpired(now)) {
                userRepository.save(user);
            }
        });

        if (trackedUserOpt.isPresent()) {
            User trackedUser = trackedUserOpt.get();
            if (trackedUser.isLoginBlocked(now)) {
                LocalDateTime blockedUntil = trackedUser.getLoginBlockedUntil();
                throw new TooManyRequestException(
                        "Terlalu banyak percobaan login yang gagal. Silakan coba lagi dalam "
                                + parseUserLockedUntil(blockedUntil));
            }
        }

        if (trackedUserOpt.isEmpty() && loginAttemptService.isLocked(credential)) {
            LocalDateTime lockoutUntil = loginAttemptService.getLockoutUntil(credential);
            throw new TooManyRequestException(
                    "Terlalu banyak percobaan login yang gagal. Silakan coba lagi dalam "
                            + parseUserLockedUntil(lockoutUntil));
        }

        assertLoginStatusAllowed(credential);

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(credential, request.getPassword()));
        } catch (AuthenticationException ex) {
            if (trackedUserOpt.isPresent()) {
                User trackedUser = trackedUserOpt.get();
                trackedUser.registerFailedLoginAttempt(now);
                userRepository.save(trackedUser);
            } else {
                loginAttemptService.recordFailedAttempt(credential);
            }
            throw ex;
        }

        loginAttemptService.resetAttempts(credential);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User trackedUser = trackedUserOpt
                .or(() -> userRepository.findById(userDetails.getId()))
                .orElse(null);
        if (trackedUser != null) {
            trackedUser.recordSuccessfulLogin(now);
            userRepository.save(trackedUser);
        }

        if (!ACTIVE_STATUS.name().equals(userDetails.getStatus())) {
            throw new ForbiddenException("Akun belum aktif atau dibatasi. Silakan hubungi admin.");
        }

        String accessToken = jwtUtils.generateAccessToken(userDetails);
        String refreshToken = refreshTokenService.createRefreshToken(userDetails.getUsername());

        AuthResponse response = authMapper.toAuthResponse(accessToken, refreshToken, userDetails);
        response.setRedirectUrl(redirectUrl);
        return response;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Login Admin (only allow users with admin/staff roles to log in)
    // ──────────────────────────────────────────────────────────────────────────
    /**
     * Authenticates the user and issues access + refresh tokens, but only if the
     * user has an admin/staff role.
     *
     * @param request     username + password
     * @param redirectUrl optional frontend URL to echo back in the response
     * @return {@link AuthResponse} carrying both tokens and the redirect URL
     * @throws BadRequestException if credentials are valid but user does not have
     *                             an admin/staff role
     */
    public AuthResponse loginAdmin(LoginRequest request, String redirectUrl) {
        AuthResponse authResponse = login(request, redirectUrl);
        String role = authResponse.getRole();
        if (!ADMIN_ASSIGNABLE_ROLES.contains(RoleName.valueOf(role))) {
            throw new UnauthorizedException("Pengguna tidak memiliki peran owner/admin/staff: " + role);
        }
        return authResponse;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Register (public — always creates a GUEST)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Self-registration endpoint. Always assigns the {@code GUEST} role and sets
     * user status to {@code AKTIF}.
     *
     * @param request username + email + password
     * @return {@link RegisterResponse} with the created user and a success message
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        assertUsernameAndEmailFree(request);
        validatePassword(request.getPassword());

        Role role = requireRole(RoleName.GUEST);
        UserStatus status = requireUserStatus(UserStatusName.AKTIF);
        User user = buildUser(request, role, status);
        userRepository.save(user);

        return RegisterResponse.builder()
                .user(userMapper.toDto(user))
                .message("Pengguna " + user.getUsername() + " berhasil dibuat")
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Register Admin (privileged — STAF_LAPANGAN / STAF_TOKO / OWNER / ADMIN)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Admin-portal registration. {@code request.role} must be one of the
     * {@link #ADMIN_ASSIGNABLE_ROLES}. Newly created users are marked as
     * {@code PENDING} until approved by an ADMIN.
     *
     * @param request username + email + password + role name
     * @return {@link RegisterResponse} with the created user and a success message
     */
    @Transactional
    public RegisterResponse registerAdmin(RegisterRequest request) {
        assertUsernameAndEmailFree(request);
        validatePassword(request.getPassword());

        RoleName roleName = parseAdminRole(request.getRole());
        Role role = requireRole(roleName);
        UserStatus status = requireUserStatus(UserStatusName.PENDING);
        User user = buildUser(request, role, status);
        userRepository.save(user);

        return RegisterResponse.builder()
                .user(userMapper.toDto(user))
                .message("Pengguna " + user.getUsername() + " berhasil diajukan dan menunggu persetujuan admin")
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Logout
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Blacklists the access token and revokes the refresh token.
     *
     * @param authorizationHeader raw {@code Authorization} header value ("Bearer
     *                            &lt;token&gt;")
     */
    @Transactional
    public void logout(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            jwtTokenBlacklist.blacklistToken(token);
            String username = jwtUtils.extractUsername(token);

            userRepository.findByUsername(username).ifPresent(user -> {
                user.recordLogout(LocalDateTime.now(ZONE_JAKARTA));
                userRepository.save(user);
            });

            refreshTokenService.deleteRefreshTokenByUsername(username);
        }
        SecurityContextHolder.clearContext();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Account Deactivation (soft delete - using user status: NON_AKTIF)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Deactivates the currently authenticated user's account by setting their
     * status
     * to NON_AKTIF. Also blacklists the current access token and revokes any
     * refresh tokens.
     *
     * @param authorizationHeader raw {@code Authorization} header value ("Bearer
     *                            &lt;token&gt;")
     */
    @Transactional
    public void deactivateAccount(String authorizationHeader) {
        // Validate the presence and format of the Authorization header
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new BadRequestException(
                    "Kegagalan karena Authorization header tidak diberikan atau tidak dimulai dengan 'Bearer '");
        }

        // Validasi token dan pastikan token belum diblacklist
        String token = authorizationHeader.substring(7);
        if (jwtTokenBlacklist.isTokenBlacklisted(token)) {
            throw new BadRequestException("Token tidak valid atau sudah diblacklist");
        }

        // Ekstrak username dari token dan cari user terkait di database
        String username = jwtUtils.extractUsername(token);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("User tidak ditemukan: " + username));

        // Pastikan token valid sebelum melanjutkan (misalnya, cek signature dan
        // expiration)
        if (!jwtUtils.validateToken(token, userDetailsService.loadUserByUsername(username))) {
            throw new BadRequestException("Token tidak valid");
        }

        // Set status user menjadi NON_AKTIF dan simpan perubahan ke database
        user.setStatus(requireUserStatus(UserStatusName.NON_AKTIF));
        userRepository.save(user);

        // Blacklist token yang digunakan untuk deaktivasi dan hapus refresh token
        // terkait
        logout(authorizationHeader);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Refresh Token
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Issues a new access token using a valid refresh token.
     *
     * @param request carries the existing refresh token
     * @return {@link AuthResponse} with a fresh access token
     */
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String username = refreshTokenService.validateRefreshToken(request.getRefreshToken());

        var userDetails = userDetailsService.loadUserByUsername(username);
        String newAccessToken = jwtUtils.generateAccessToken(userDetails);

        String role = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("UNKNOWN");

        return authMapper.toRefreshedAuthResponse(
                newAccessToken, request.getRefreshToken(), username, role);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Ensures the requested username and email are not already taken by another
     * user.
     * 
     * @param request registration request containing the desired username and email
     * @throws ConflictException if the username or email is already taken
     */
    private void assertUsernameAndEmailFree(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username sudah digunakan: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email sudah terdaftar: " + request.getEmail());
        }
    }

    /**
     * Fetches the {@link Role} entity for the given {@link RoleName}, throwing an
     * exception if not found.
     * 
     * @param name the role name to look up
     * @return the corresponding Role entity
     * @throws BadRequestException if the role is not found in the database
     */
    private Role requireRole(RoleName name) {
        return roleRepository.findByRoleName(name)
                .orElseThrow(() -> new BadRequestException("Peran tidak ditemukan di database: " + name));
    }

    /**
     * Fetches a persisted {@link UserStatus} row by enum name.
     *
     * @param statusName status enum to load
     * @return persisted status entity
     * @throws BadRequestException if status rows are not seeded properly
     */
    private UserStatus requireUserStatus(UserStatusName statusName) {
        return userStatusRepository.findByName(statusName)
                .orElseThrow(
                        () -> new BadRequestException("Status pengguna tidak ditemukan di database: " + statusName));
    }

    /**
     * If the credential belongs to an existing user, deny login unless status is
     * AKTIF.
     */
    private void assertLoginStatusAllowed(String credential) {
        userRepository.findByUsername(credential)
                .or(() -> userRepository.findByEmail(credential))
                .ifPresent(user -> {
                    if (user.getStatus() == null || !ACTIVE_STATUS.equals(user.getStatus().getName())) {
                        throw new ForbiddenException("Akun belum aktif atau dibatasi. Silakan hubungi admin.");
                    }
                });
    }

    /**
     * Finds a user by username or email. Used for login tracking (failed attempts,
     * last login/logout timestamps) even when the credential is invalid for
     * authentication purposes. This allows us to implement features like account
     * lockout after too many failed attempts, even if the attacker is trying
     */
    private Optional<User> findUserByCredential(String credential) {
        return userRepository.findByUsername(credential)
                .or(() -> userRepository.findByEmail(credential));
    }

    /**
     * Builds a new User entity from the registration request and assigned role,
     * encoding the password.
     * 
     * @param request the registration request containing username, email, and raw
     *                password
     * @param role    the Role entity to assign to the new user
     * @return a new User entity ready to be saved to the database
     */
    private User buildUser(RegisterRequest request, Role role, UserStatus status) {
        return User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .status(status)
                .build();
    }

    /**
     * Validates password strength: minimum 8 characters, at least one uppercase
     * letter,
     * and at least one digit.
     *
     * @param password the raw password to validate
     * @throws BadRequestException if the password does not meet the
     *                             requirements
     */
    private void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 20) {
            throw new BadRequestException("Password harus terdiri dari minimal 8 dan maksimal 20 karakter.");
        }
        if (password.chars().noneMatch(Character::isUpperCase)) {
            throw new BadRequestException("Password harus mengandung setidaknya satu huruf kapital.");
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            throw new BadRequestException("Password harus mengandung setidaknya satu angka.");
        }
        if (password.chars().anyMatch(Character::isWhitespace)) {
            throw new BadRequestException("Password tidak boleh mengandung karakter spasi.");
        }
        // Special characters are not allowed, only "_", "-", "@", and "." are permitted
        if (password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch)
                && ch != '_' && ch != '-' && ch != '@' && ch != '.')) {
            throw new BadRequestException(
                    "Password mengandung karakter tidak valid. Hanya huruf, angka, dan _ - @ . yang diperbolehkan.");
        }
    }

    /**
     * Parses and validates the requested admin role from the registration request,
     * ensuring it is one of the allowed roles.
     * 
     * @param rawRole the raw role name string from the registration request
     * @return the corresponding RoleName enum value if valid
     * @throws BadRequestException if the role is blank, unknown, or not
     *                             allowed for admin registration
     */
    private RoleName parseAdminRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            throw new BadRequestException("Peran diperlukan untuk registrasi admin.");
        }
        try {
            RoleName parsed = RoleName.valueOf(rawRole.toUpperCase());
            if (!ADMIN_ASSIGNABLE_ROLES.contains(parsed)) {
                throw new BadRequestException(
                        "Peran '" + rawRole + "' tidak dapat diberikan melalui registrasi admin.");
            }
            return parsed;
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Role " + rawRole + " tidak valid.");
        }
    }

    /**
     * 
     */
    private String parseUserLockedUntil(LocalDateTime lockedUntil) {
        if (lockedUntil == null) {
            return "waktu tidak diketahui";
        }
        if (lockedUntil.isBefore(LocalDateTime.now(ZONE_JAKARTA))) {
            return "baru saja";
        }
        Duration durationToReset = Duration.between(LocalDateTime.now(ZONE_JAKARTA), lockedUntil);
        if (durationToReset.isNegative() || durationToReset.isZero()) {
            return "beberapa detik lagi";
        }

        if (durationToReset.toMinutes() < 1) {
            long seconds = durationToReset.getSeconds();
            return seconds + " detik";
        } else if (durationToReset.toHours() < 1) {
            long minutes = durationToReset.toMinutes();
            return minutes + " menit" +
                    (durationToReset.getSeconds() % 60 > 0 ? " dan beberapa detik" : "");
        } else if (durationToReset.toDays() < 1) {
            long hours = durationToReset.toHours();
            return hours + " jam" +
                    (durationToReset.toMinutes() % 60 > 0 ? " dan beberapa menit" : "");
        } else {
            long days = durationToReset.toDays();
            return days + " hari" +
                    (durationToReset.toHours() % 24 > 0 ? " dan beberapa jam" : "");
        }
    }
}
