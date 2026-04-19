package io.mpruy.gor_gemilangcondet.backend_api.entities.users;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    private static final ZoneId ZONE_JAKARTA = ZoneId.of("Asia/Jakarta");
    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final long FAILED_LOGIN_RESET_WINDOW_MINUTES = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    private LocalDateTime membershipStart;

    private LocalDateTime membershipEnd;

    @Column(nullable = false)
    private String password;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "status_id", nullable = false)
    private UserStatus status;

    /** Timestamp of when account was marked BANNED; null otherwise. */
    private LocalDateTime bannedAt;

    private LocalDateTime lastLoginAt;

    private LocalDateTime lastLogoutAt;

    @Builder.Default
    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    private LocalDateTime failedLoginWindowStartedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime statusChangedAt;

    @Transient
    private UserStatusName previousStatusName;

    @PrePersist
    private void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZONE_JAKARTA);
        createdAt = now;
        failedLoginAttempts = clampFailedLoginAttempts(failedLoginAttempts);
        if (failedLoginAttempts == 0) {
            failedLoginWindowStartedAt = null;
        }
        syncBannedTimestamp(now);
    }

    @PreUpdate
    private void onUpdate() {
        LocalDateTime now = LocalDateTime.now(ZONE_JAKARTA);
        failedLoginAttempts = clampFailedLoginAttempts(failedLoginAttempts);
        if (failedLoginAttempts == 0) {
            failedLoginWindowStartedAt = null;
        }

        if (hasStatusChanged()) {
            statusChangedAt = now;
        } else {
            updatedAt = now;
        }
        syncBannedTimestamp(now);
    }

    @PostLoad
    @PostPersist
    @PostUpdate
    private void captureCurrentStatusSnapshot() {
        previousStatusName = getCurrentStatusName();
    }

    public boolean isLoginBlocked(LocalDateTime now) {
        resetFailedLoginAttemptsIfWindowExpired(now);
        return failedLoginAttempts >= MAX_FAILED_LOGIN_ATTEMPTS;
    }

    public LocalDateTime getLoginBlockedUntil() {
        if (failedLoginWindowStartedAt == null || failedLoginAttempts < MAX_FAILED_LOGIN_ATTEMPTS) {
            return null;
        }
        return failedLoginWindowStartedAt.plusMinutes(FAILED_LOGIN_RESET_WINDOW_MINUTES);
    }

    public void registerFailedLoginAttempt(LocalDateTime now) {
        resetFailedLoginAttemptsIfWindowExpired(now);
        if (failedLoginWindowStartedAt == null) {
            failedLoginWindowStartedAt = now;
        }
        failedLoginAttempts = Math.min(failedLoginAttempts + 1, MAX_FAILED_LOGIN_ATTEMPTS);
    }

    public void recordSuccessfulLogin(LocalDateTime now) {
        lastLoginAt = now;
        failedLoginAttempts = 0;
        failedLoginWindowStartedAt = null;
    }

    public void recordLogout(LocalDateTime now) {
        lastLogoutAt = now;
    }

    public boolean resetFailedLoginAttemptsIfWindowExpired(LocalDateTime now) {
        if (failedLoginWindowStartedAt == null) {
            return false;
        }

        LocalDateTime resetAt = failedLoginWindowStartedAt.plusMinutes(FAILED_LOGIN_RESET_WINDOW_MINUTES);
        if (now.isBefore(resetAt)) {
            return false;
        }

        failedLoginAttempts = 0;
        failedLoginWindowStartedAt = null;
        return true;
    }

    private boolean hasStatusChanged() {
        UserStatusName currentStatus = getCurrentStatusName();
        return previousStatusName != currentStatus;
    }

    private UserStatusName getCurrentStatusName() {
        return status == null ? null : status.getName();
    }

    private int clampFailedLoginAttempts(int attempts) {
        if (attempts < 0) {
            return 0;
        }
        return Math.min(attempts, MAX_FAILED_LOGIN_ATTEMPTS);
    }

    private void syncBannedTimestamp(LocalDateTime now) {
        if (status != null && status.getName() == UserStatusName.BANNED) {
            if (bannedAt == null) {
                bannedAt = now;
            }
            return;
        }
        bannedAt = null;
    }
}
