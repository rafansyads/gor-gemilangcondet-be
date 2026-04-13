package io.mpruy.gor_gemilangcondet.backend_api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks failed login attempts per credential (username or email) and enforces
 * a 1-minute lockout after {@value #MAX_ATTEMPTS} consecutive failures.
 */
@Service
@Slf4j
public class LoginAttemptService {

    static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MINUTES = 1;

    private final ConcurrentHashMap<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    /**
     * Returns true if the given credential is currently locked out.
     */
    public boolean isLocked(String credential) {
        AttemptRecord record = attempts.get(normalize(credential));
        if (record == null)
            return false;
        if (record.lockoutUntil != null && LocalDateTime.now().isBefore(record.lockoutUntil)) {
            return true;
        }
        // Lockout has expired — evict eagerly
        if (record.lockoutUntil != null) {
            attempts.remove(normalize(credential));
        }
        return false;
    }

    /**
     * Returns the time at which the lockout for the given credential expires,
     * or {@code null} if the credential is not locked.
     */
    public LocalDateTime getLockoutUntil(String credential) {
        AttemptRecord record = attempts.get(normalize(credential));
        if (record == null || record.lockoutUntil == null)
            return null;
        return LocalDateTime.now().isBefore(record.lockoutUntil) ? record.lockoutUntil : null;
    }

    /**
     * Records a failed login attempt. Locks out the credential after
     * {@value #MAX_ATTEMPTS} failures.
     */
    public void recordFailedAttempt(String credential) {
        String key = normalize(credential);
        AttemptRecord record = attempts.computeIfAbsent(key, k -> new AttemptRecord());
        record.count++;
        if (record.count >= MAX_ATTEMPTS) {
            record.lockoutUntil = LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES);
            log.warn("Login locked for credential '{}' until {}", key, record.lockoutUntil);
        }
    }

    /**
     * Resets the failed-attempt counter on a successful login.
     */
    public void resetAttempts(String credential) {
        attempts.remove(normalize(credential));
    }

    /**
     * Removes all entries whose lockout period has expired.
     * Called periodically by
     * {@link io.mpruy.gor_gemilangcondet.backend_api.config.scheduler.AuthScheduler}.
     */
    public void cleanupExpiredLockouts() {
        LocalDateTime now = LocalDateTime.now();
        int removed = 0;
        var it = attempts.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            AttemptRecord record = entry.getValue();
            if (record.lockoutUntil != null && now.isAfter(record.lockoutUntil)) {
                it.remove();
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("AuthScheduler: removed {} expired login lockout(s)", removed);
        }
    }

    private static String normalize(String credential) {
        return credential == null ? "" : credential.toLowerCase();
    }

    private static class AttemptRecord {
        int count = 0;
        LocalDateTime lockoutUntil = null;
    }
}
