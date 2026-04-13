package io.mpruy.gor_gemilangcondet.backend_api.service;

import io.mpruy.gor_gemilangcondet.backend_api.dto.users.UserDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.users.requests.UpdateProfileRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.users.responses.UpdateProfileResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.Role;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.RoleName;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.User;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatusName;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ConflictException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ResourceNotFoundException;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserStatusRepository;
import io.mpruy.gor_gemilangcondet.backend_api.security.UserDetailsImpl;
import io.mpruy.gor_gemilangcondet.backend_api.security.jwt.JwtUtils;
import io.mpruy.gor_gemilangcondet.backend_api.security.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserStatusRepository userStatusRepository;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UUID userId;
    private Role guestRole;
    private UserStatus activeStatus;
    private UserStatus pendingStatus;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        guestRole = Role.builder().id(1).roleName(RoleName.GUEST).build();
        activeStatus = UserStatus.builder().id(1).name(UserStatusName.AKTIF).build();
        pendingStatus = UserStatus.builder().id(2).name(UserStatusName.PENDING).build();
        testUser = User.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .password("encoded")
                .role(guestRole)
                .status(activeStatus)
                .build();
    }

    @Nested
    @DisplayName("Get All Users Tests")
    class GetAllUsersTests {

        @Test
        @DisplayName("Should return all users")
        void getAllUsers_Success() {
            User user2 = User.builder()
                    .id(UUID.randomUUID()).username("user2").email("u2@test.com")
                    .password("enc").role(guestRole).status(activeStatus).build();

            when(userRepository.findAll()).thenReturn(List.of(testUser, user2));

            List<UserDto> result = userService.getAllUsers();

            assertEquals(2, result.size());
            assertEquals("testuser", result.get(0).getUsername());
        }

        @Test
        @DisplayName("Should return empty list when no users")
        void getAllUsers_Empty() {
            when(userRepository.findAll()).thenReturn(Collections.emptyList());

            List<UserDto> result = userService.getAllUsers();

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Get User By ID Tests")
    class GetUserByIdTests {

        @Test
        @DisplayName("Should return user by ID")
        void getUserById_Success() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            UserDto result = userService.getUserById(userId);

            assertNotNull(result);
            assertEquals("testuser", result.getUsername());
            assertEquals("test@example.com", result.getEmail());
            assertEquals(RoleName.GUEST, result.getRole());
            assertEquals(UserStatusName.AKTIF.name(), result.getStatus());
        }

        @Test
        @DisplayName("Should throw exception when user not found by ID")
        void getUserById_NotFound() {
            UUID randomId = UUID.randomUUID();
            when(userRepository.findById(randomId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(randomId));
        }
    }

    @Nested
    @DisplayName("Pending Admin Registration Tests")
    class PendingAdminRegistrationTests {

        @Test
        @DisplayName("Should return pending admin/staff registrations")
        void getPendingAdminRegistrations_Success() {
            User pendingStaff = User.builder()
                    .id(UUID.randomUUID())
                    .username("staf_pending")
                    .email("staf_pending@test.com")
                    .password("encoded")
                    .role(Role.builder().id(3).roleName(RoleName.STAF_LAPANGAN).build())
                    .status(pendingStatus)
                    .build();

            when(userRepository.findByStatus_NameAndRole_RoleNameIn(eq(UserStatusName.PENDING), anyCollection()))
                    .thenReturn(List.of(pendingStaff));

            List<UserDto> result = userService.getPendingAdminRegistrations();

            assertEquals(1, result.size());
            assertEquals("staf_pending", result.get(0).getUsername());
            assertEquals(UserStatusName.PENDING.name(), result.get(0).getStatus());
        }

        @Test
        @DisplayName("Should approve pending admin/staff registration")
        void approvePendingAdminRegistration_Success() {
            UUID pendingId = UUID.randomUUID();
            User pendingStaff = User.builder()
                    .id(pendingId)
                    .username("staf_pending")
                    .email("staf_pending@test.com")
                    .password("encoded")
                    .role(Role.builder().id(3).roleName(RoleName.STAF_LAPANGAN).build())
                    .status(pendingStatus)
                    .build();

            when(userRepository.findByIdAndStatus_NameAndRole_RoleNameIn(
                    eq(pendingId), eq(UserStatusName.PENDING), anyCollection()))
                    .thenReturn(Optional.of(pendingStaff));
            when(userStatusRepository.findByName(UserStatusName.AKTIF)).thenReturn(Optional.of(activeStatus));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            UserDto result = userService.approvePendingAdminRegistration(pendingId);

            assertEquals(UserStatusName.AKTIF.name(), result.getStatus());
            assertNull(pendingStaff.getBannedAt());
        }

        @Test
        @DisplayName("Should reject pending admin/staff registration")
        void rejectPendingAdminRegistration_Success() {
            UUID pendingId = UUID.randomUUID();
            UserStatus bannedStatus = UserStatus.builder().id(3).name(UserStatusName.BANNED).build();
            User pendingStaff = User.builder()
                    .id(pendingId)
                    .username("staf_pending")
                    .email("staf_pending@test.com")
                    .password("encoded")
                    .role(Role.builder().id(3).roleName(RoleName.STAF_LAPANGAN).build())
                    .status(pendingStatus)
                    .build();

            when(userRepository.findByIdAndStatus_NameAndRole_RoleNameIn(
                    eq(pendingId), eq(UserStatusName.PENDING), anyCollection()))
                    .thenReturn(Optional.of(pendingStaff));
            when(userStatusRepository.findByName(UserStatusName.BANNED)).thenReturn(Optional.of(bannedStatus));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            UserDto result = userService.rejectPendingAdminRegistration(pendingId);

            assertEquals(UserStatusName.BANNED.name(), result.getStatus());
            assertNotNull(pendingStaff.getBannedAt());
            verify(userRepository).save(pendingStaff);
        }

        @Test
        @DisplayName("Should throw when approving non-existing pending registration")
        void approvePendingAdminRegistration_NotFound() {
            UUID pendingId = UUID.randomUUID();

            when(userRepository.findByIdAndStatus_NameAndRole_RoleNameIn(
                    eq(pendingId), eq(UserStatusName.PENDING), anyCollection()))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> userService.approvePendingAdminRegistration(pendingId));
        }

        @Test
        @DisplayName("Should delete banned users older than 3 days")
        void deleteExpiredBannedUsers_Success() {
            User oldBanned = User.builder()
                    .id(UUID.randomUUID())
                    .username("old_banned")
                    .email("old_banned@test.com")
                    .password("encoded")
                    .role(Role.builder().id(3).roleName(RoleName.STAF_LAPANGAN).build())
                    .status(UserStatus.builder().id(3).name(UserStatusName.BANNED).build())
                    .bannedAt(LocalDateTime.now().minusDays(4))
                    .build();

            when(userRepository.findByStatus_NameAndBannedAtLessThanEqual(eq(UserStatusName.BANNED), any(LocalDateTime.class)))
                    .thenReturn(List.of(oldBanned));

            int deleted = userService.deleteExpiredBannedUsers();

            assertEquals(1, deleted);
            verify(refreshTokenService).deleteRefreshTokenByUsername("old_banned");
            verify(userRepository).deleteAllInBatch(List.of(oldBanned));
        }
    }

    @Nested
    @DisplayName("Get User By Username Tests")
    class GetUserByUsernameTests {

        @Test
        @DisplayName("Should return user by username")
        void getUserByUsername_Success() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            UserDto result = userService.getUserByUsername("testuser");

            assertNotNull(result);
            assertEquals("testuser", result.getUsername());
        }

        @Test
        @DisplayName("Should throw exception when user not found by username")
        void getUserByUsername_NotFound() {
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> userService.getUserByUsername("nonexistent"));
        }
    }

    @Nested
    @DisplayName("Update Profile Tests")
    class UpdateProfileTests {

        @BeforeEach
        void setUpAuth() {
            UserDetailsImpl userDetails = new UserDetailsImpl(testUser);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null,
                    userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        @Test
        @DisplayName("Should update profile successfully")
        void updateProfile_Success() {
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setUsername("updateduser");
            request.setEmail("updated@example.com");

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByUsernameAndIdNot("updateduser", userId)).thenReturn(false);
            when(userRepository.existsByEmailAndIdNot("updated@example.com", userId)).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            UpdateProfileResponse response = userService.updateProfile(request);
            UserDto updatedUser = response.getUser();

            assertEquals("updateduser", updatedUser.getUsername());
            assertEquals("updated@example.com", updatedUser.getEmail());
        }

        @Test
        @DisplayName("Should throw when username is already taken")
        void updateProfile_DuplicateUsername() {
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setUsername("taken");
            request.setEmail("updated@example.com");

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByUsernameAndIdNot("taken", userId)).thenReturn(true);

            assertThrows(ConflictException.class, () -> userService.updateProfile(request));
        }

        @Test
        @DisplayName("Should throw when email is already registered")
        void updateProfile_DuplicateEmail() {
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setUsername("newname");
            request.setEmail("taken@example.com");

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByUsernameAndIdNot("newname", userId)).thenReturn(false);
            when(userRepository.existsByEmailAndIdNot("taken@example.com", userId)).thenReturn(true);

            assertThrows(ConflictException.class, () -> userService.updateProfile(request));
        }

        @Test
        @DisplayName("Should throw when current user not found")
        void updateProfile_UserNotFound() {
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setUsername("newname");
            request.setEmail("new@example.com");

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> userService.updateProfile(request));
        }
    }
}
