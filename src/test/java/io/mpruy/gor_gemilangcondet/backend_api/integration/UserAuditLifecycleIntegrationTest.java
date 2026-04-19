package io.mpruy.gor_gemilangcondet.backend_api.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import io.mpruy.gor_gemilangcondet.backend_api.entities.users.Role;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.RoleName;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.User;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatusName;
import io.mpruy.gor_gemilangcondet.backend_api.repository.RoleRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserStatusRepository;

@DataJpaTest
@ImportAutoConfiguration(CacheAutoConfiguration.class)
@TestPropertySource(properties = "spring.cache.type=none")
class UserAuditLifecycleIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserStatusRepository userStatusRepository;

    private Role guestRole;
    private UserStatus activeStatus;
    private UserStatus suspendedStatus;
    private UserStatus bannedStatus;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        guestRole = roleRepository.findByRoleName(RoleName.GUEST)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(RoleName.GUEST).build()));

        activeStatus = userStatusRepository.findByName(UserStatusName.AKTIF)
                .orElseGet(() -> userStatusRepository.save(UserStatus.builder().name(UserStatusName.AKTIF).build()));

        suspendedStatus = userStatusRepository.findByName(UserStatusName.SUSPENDED)
                .orElseGet(
                        () -> userStatusRepository.save(UserStatus.builder().name(UserStatusName.SUSPENDED).build()));

        bannedStatus = userStatusRepository.findByName(UserStatusName.BANNED)
                .orElseGet(() -> userStatusRepository.save(UserStatus.builder().name(UserStatusName.BANNED).build()));
    }

    @Test
    @DisplayName("User lifecycle should populate createdAt/updatedAt/statusChangedAt and bannedAt correctly")
    void userLifecycle_ShouldTrackAuditTimestamps() {
        User user = User.builder()
                .username("audit_user")
                .email("audit_user@test.com")
                .password("encoded")
                .role(guestRole)
                .status(activeStatus)
                .build();

        user = userRepository.saveAndFlush(user);

        assertNotNull(user.getCreatedAt());
        assertNull(user.getUpdatedAt());
        assertNull(user.getStatusChangedAt());
        assertEquals(0, user.getFailedLoginAttempts());

        user.setEmail("audit_user_updated@test.com");
        user = userRepository.saveAndFlush(user);

        LocalDateTime updatedAt = user.getUpdatedAt();
        assertNotNull(updatedAt);
        assertNull(user.getStatusChangedAt());

        user.setStatus(suspendedStatus);
        user = userRepository.saveAndFlush(user);

        assertNotNull(user.getStatusChangedAt());
        assertEquals(updatedAt, user.getUpdatedAt());
        assertNull(user.getBannedAt());

        LocalDateTime firstStatusChangedAt = user.getStatusChangedAt();
        user.setStatus(bannedStatus);
        user = userRepository.saveAndFlush(user);

        assertNotNull(user.getBannedAt());
        assertTrue(!user.getStatusChangedAt().isBefore(firstStatusChangedAt));
    }

    @Test
    @DisplayName("Failed login attempts should cap at 5 and reset after 1 minute")
    void failedLoginAttempts_ShouldCapAndResetAfterOneMinute() {
        User user = User.builder()
                .username("lock_user")
                .email("lock_user@test.com")
                .password("encoded")
                .role(guestRole)
                .status(activeStatus)
                .build();

        LocalDateTime baseTime = LocalDateTime.of(2026, 1, 1, 10, 0);
        for (int i = 0; i < 7; i++) {
            user.registerFailedLoginAttempt(baseTime.plusSeconds(i));
        }

        assertEquals(5, user.getFailedLoginAttempts());
        assertTrue(user.isLoginBlocked(baseTime.plusSeconds(30)));

        user.resetFailedLoginAttemptsIfWindowExpired(baseTime.plusMinutes(1).plusSeconds(1));

        assertEquals(0, user.getFailedLoginAttempts());
        assertTrue(!user.isLoginBlocked(baseTime.plusMinutes(2)));
    }
}
