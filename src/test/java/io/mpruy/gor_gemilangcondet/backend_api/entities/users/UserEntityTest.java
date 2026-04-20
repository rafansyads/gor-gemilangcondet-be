package io.mpruy.gor_gemilangcondet.backend_api.entities.users;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserEntityTest {

    private User buildUser(UserStatusName statusName) {
        Role role = Role.builder().roleName(RoleName.MEMBER).build();
        UserStatus status = UserStatus.builder().name(statusName).build();

        return User.builder()
                .username("member")
                .email("member@example.com")
                .password("secret")
                .role(role)
                .status(status)
                .build();
    }

    @Test
    @DisplayName("onCreate should clamp failed attempts and set timestamps")
    void onCreate_InitializesState() {
        User user = buildUser(UserStatusName.AKTIF);
        user.setFailedLoginAttempts(-3);

        ReflectionTestUtils.invokeMethod(user, "onCreate");

        assertNotNull(user.getCreatedAt());
        assertTrue(user.getFailedLoginAttempts() == 0);
        assertNull(user.getFailedLoginWindowStartedAt());
        assertNull(user.getBannedAt());
    }

    @Test
    @DisplayName("onCreate should set bannedAt when status is BANNED")
    void onCreate_BannedStatusSetsTimestamp() {
        User user = buildUser(UserStatusName.BANNED);

        ReflectionTestUtils.invokeMethod(user, "onCreate");

        assertNotNull(user.getBannedAt());
    }

    @Test
    @DisplayName("failed login tracking should block then reset after window")
    void failedLoginBlockingAndReset() {
        User user = buildUser(UserStatusName.AKTIF);
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < 5; i++) {
            user.registerFailedLoginAttempt(now.plusSeconds(i));
        }

        assertTrue(user.isLoginBlocked(now.plusSeconds(30)));
        assertNotNull(user.getLoginBlockedUntil());

        boolean reset = user.resetFailedLoginAttemptsIfWindowExpired(now.plusMinutes(2));
        assertTrue(reset);
        assertFalse(user.isLoginBlocked(now.plusMinutes(2).plusSeconds(1)));
    }

    @Test
    @DisplayName("successful login and logout should set corresponding timestamps")
    void recordLoginAndLogout() {
        User user = buildUser(UserStatusName.AKTIF);
        LocalDateTime loginTime = LocalDateTime.now();
        LocalDateTime logoutTime = loginTime.plusMinutes(5);

        user.registerFailedLoginAttempt(loginTime.minusSeconds(10));
        user.recordSuccessfulLogin(loginTime);
        user.recordLogout(logoutTime);

        assertNotNull(user.getLastLoginAt());
        assertNotNull(user.getLastLogoutAt());
        assertTrue(user.getFailedLoginAttempts() == 0);
        assertNull(user.getFailedLoginWindowStartedAt());
    }

    @Test
    @DisplayName("onUpdate should set updatedAt when status unchanged")
    void onUpdate_StatusUnchanged() {
        User user = buildUser(UserStatusName.AKTIF);
        ReflectionTestUtils.invokeMethod(user, "captureCurrentStatusSnapshot");

        ReflectionTestUtils.invokeMethod(user, "onUpdate");

        assertNotNull(user.getUpdatedAt());
        assertNull(user.getStatusChangedAt());
    }

    @Test
    @DisplayName("onUpdate should set statusChangedAt when status changed")
    void onUpdate_StatusChanged() {
        User user = buildUser(UserStatusName.AKTIF);
        ReflectionTestUtils.invokeMethod(user, "captureCurrentStatusSnapshot");

        user.setStatus(UserStatus.builder().name(UserStatusName.BANNED).build());
        ReflectionTestUtils.invokeMethod(user, "onUpdate");

        assertNotNull(user.getStatusChangedAt());
        assertNotNull(user.getBannedAt());
    }
}
