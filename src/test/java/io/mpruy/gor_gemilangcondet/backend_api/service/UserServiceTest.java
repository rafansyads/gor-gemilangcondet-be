package io.mpruy.gor_gemilangcondet.backend_api.service;

import io.mpruy.gor_gemilangcondet.backend_api.dto.users.UserDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.users.requests.UpdateProfileRequest;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.Role;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.RoleName;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.User;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ConflictException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ResourceNotFoundException;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserRepository;
import io.mpruy.gor_gemilangcondet.backend_api.security.UserDetailsImpl;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UUID userId;
    private Role guestRole;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        guestRole = Role.builder().id(1).roleName(RoleName.GUEST).build();
        testUser = User.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .password("encoded")
                .role(guestRole)
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET ALL USERS
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Get All Users Tests")
    class GetAllUsersTests {

        @Test
        @DisplayName("Should return all users")
        void getAllUsers_Success() {
            User user2 = User.builder()
                    .id(UUID.randomUUID()).username("user2").email("u2@test.com")
                    .password("enc").role(guestRole).build();

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

    // ══════════════════════════════════════════════════════════════════════════
    // GET USER BY ID
    // ══════════════════════════════════════════════════════════════════════════

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
        }

        @Test
        @DisplayName("Should throw exception when user not found by ID")
        void getUserById_NotFound() {
            UUID randomId = UUID.randomUUID();
            when(userRepository.findById(randomId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(randomId));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET USER BY USERNAME
    // ══════════════════════════════════════════════════════════════════════════

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

    // ══════════════════════════════════════════════════════════════════════════
    // UPDATE PROFILE
    // ══════════════════════════════════════════════════════════════════════════

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

            UserDto result = userService.updateProfile(request);

            assertEquals("updateduser", result.getUsername());
            assertEquals("updated@example.com", result.getEmail());
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
