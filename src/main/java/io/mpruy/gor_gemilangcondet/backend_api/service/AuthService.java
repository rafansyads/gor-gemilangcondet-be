package io.mpruy.gor_gemilangcondet.backend_api.service;

import java.util.Set;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
import io.mpruy.gor_gemilangcondet.backend_api.exception.BadRequestException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ConflictException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.UnauthorizedException;
import io.mpruy.gor_gemilangcondet.backend_api.repository.RoleRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserRepository;
import io.mpruy.gor_gemilangcondet.backend_api.security.UserDetailsImpl;
import io.mpruy.gor_gemilangcondet.backend_api.security.jwt.JwtUtils;
import io.mpruy.gor_gemilangcondet.backend_api.security.service.JwtTokenBlacklist;
import io.mpruy.gor_gemilangcondet.backend_api.security.service.RefreshTokenService;
import io.mpruy.gor_gemilangcondet.backend_api.service.mapper.AuthMapper;
import io.mpruy.gor_gemilangcondet.backend_api.service.mapper.UserMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final JwtTokenBlacklist jwtTokenBlacklist;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsService userDetailsService;
    private final AuthMapper authMapper;
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Roles that only privileged actors (admin portal) may assign during
     * registration.
     */
    private static final Set<RoleName> ADMIN_ASSIGNABLE_ROLES = Set.of(
            RoleName.STAF_LAPANGAN,
            RoleName.STAF_TOKO,
            RoleName.OWNER,
            RoleName.ADMIN);

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
    public AuthResponse login(LoginRequest request, String redirectUrl) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(), request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

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
            throw new UnauthorizedException("User does not have owner/admin/staff role: " + role);
        }
        return authResponse;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Register (public — always creates a GUEST)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Self-registration endpoint. Always assigns the {@code GUEST} role.
     *
     * @param request username + email + password
     * @return {@link RegisterResponse} with the created user and a success message
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        assertUsernameAndEmailFree(request);
        validatePassword(request.getPassword());

        Role role = requireRole(RoleName.GUEST);
        User user = buildUser(request, role);
        userRepository.save(user);

        return RegisterResponse.builder()
                .user(userMapper.toDto(user))
                .message("User " + user.getUsername() + " successfully created")
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Register Admin (privileged — STAF_LAPANGAN / STAF_TOKO / OWNER / ADMIN)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Admin-portal registration. {@code request.role} must be one of the
     * {@link #ADMIN_ASSIGNABLE_ROLES}.
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
        User user = buildUser(request, role);
        userRepository.save(user);

        return RegisterResponse.builder()
                .user(userMapper.toDto(user))
                .message("User " + user.getUsername() + " successfully created")
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
    public void logout(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            jwtTokenBlacklist.blacklistToken(token);
            String username = jwtUtils.extractUsername(token);
            refreshTokenService.deleteRefreshTokenByUsername(username);
        }
        SecurityContextHolder.clearContext();
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
     * @throws IllegalArgumentException if the username or email is already taken
     */
    private void assertUsernameAndEmailFree(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username is already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email is already registered: " + request.getEmail());
        }
    }

    /**
     * Fetches the {@link Role} entity for the given {@link RoleName}, throwing an
     * exception if not found.
     * 
     * @param name the role name to look up
     * @return the corresponding Role entity
     * @throws IllegalStateException if the role is not found in the database
     */
    private Role requireRole(RoleName name) {
        return roleRepository.findByRoleName(name)
                .orElseThrow(() -> new IllegalStateException("Role not found in database: " + name));
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
    private User buildUser(RegisterRequest request, Role role) {
        return User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();
    }

    /**
     * Validates password strength: minimum 8 characters, at least one uppercase
     * letter,
     * and at least one digit.
     *
     * @param password the raw password to validate
     * @throws IllegalArgumentException if the password does not meet the
     *                                  requirements
     */
    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters long.");
        }
        if (password.chars().noneMatch(Character::isUpperCase)) {
            throw new BadRequestException("Password must contain at least one uppercase letter.");
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            throw new BadRequestException("Password must contain at least one digit.");
        }
        if (password.chars().anyMatch(Character::isWhitespace)) {
            throw new BadRequestException("Password must not contain whitespace.");
        }
        // Special characters are not allowed, only "_", "-", "@", and "." are permitted
        if (password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch)
                && ch != '_' && ch != '-' && ch != '@' && ch != '.')) {
            throw new BadRequestException(
                    "Password contains invalid characters. Only letters, digits, and _ - @ . are allowed.");
        }
    }

    /**
     * Parses and validates the requested admin role from the registration request,
     * ensuring it is one of the allowed roles.
     * 
     * @param rawRole the raw role name string from the registration request
     * @return the corresponding RoleName enum value if valid
     * @throws IllegalArgumentException if the role is blank, unknown, or not
     *                                  allowed for admin registration
     */
    private RoleName parseAdminRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            throw new BadRequestException("Role is required for admin registration.");
        }
        try {
            RoleName parsed = RoleName.valueOf(rawRole.toUpperCase());
            if (!ADMIN_ASSIGNABLE_ROLES.contains(parsed)) {
                throw new BadRequestException(
                        "Role '" + rawRole + "' cannot be assigned via admin registration.");
            }
            return parsed;
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown role: " + rawRole);
        }
    }
}
