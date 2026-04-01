package io.mpruy.gor_gemilangcondet.backend_api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptServiceTest {

    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService();
    }

    // ── isLocked ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isLocked Tests")
    class IsLockedTests {

        @Test
        @DisplayName("Returns false when no attempts have been recorded")
        void isLocked_FalseWhenNoAttempts() {
            assertFalse(service.isLocked("user"));
        }

        @Test
        @DisplayName("Returns false when attempts are below the threshold")
        void isLocked_FalseBelowThreshold() {
            for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS - 1; i++) {
                service.recordFailedAttempt("user");
            }
            assertFalse(service.isLocked("user"));
        }

        @Test
        @DisplayName("Returns true after reaching MAX_ATTEMPTS")
        void isLocked_TrueAtMaxAttempts() {
            for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
                service.recordFailedAttempt("user");
            }
            assertTrue(service.isLocked("user"));
        }

        @Test
        @DisplayName("Returns false and evicts record after lockout expiry")
        void isLocked_FalseAndEvictsAfterExpiry() {
            for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
                service.recordFailedAttempt("user");
            }
            setLockoutUntil("user", LocalDateTime.now().minusMinutes(2));

            assertFalse(service.isLocked("user"));
            assertFalse(getAttempts().containsKey("user"));
        }

        @Test
        @DisplayName("Lookup is case-insensitive")
        void isLocked_CaseInsensitive() {
            for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
                service.recordFailedAttempt("USER");
            }
            assertTrue(service.isLocked("user"));
            assertTrue(service.isLocked("User"));
            assertTrue(service.isLocked("USER"));
        }
    }

    // ── getLockoutUntil ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("getLockoutUntil Tests")
    class GetLockoutUntilTests {

        @Test
        @DisplayName("Returns null when credential is not locked")
        void getLockoutUntil_NullWhenNotLocked() {
            assertNull(service.getLockoutUntil("user"));
        }

        @Test
        @DisplayName("Returns a future timestamp when locked")
        void getLockoutUntil_FutureTimeWhenLocked() {
            for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
                service.recordFailedAttempt("user");
            }
            LocalDateTime lockoutUntil = service.getLockoutUntil("user");
            assertNotNull(lockoutUntil);
            assertTrue(lockoutUntil.isAfter(LocalDateTime.now()));
        }

        @Test
        @DisplayName("Returns null when lockout has expired")
        void getLockoutUntil_NullWhenExpired() {
            for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
                service.recordFailedAttempt("user");
            }
            setLockoutUntil("user", LocalDateTime.now().minusMinutes(2));

            assertNull(service.getLockoutUntil("user"));
        }
    }

    // ── recordFailedAttempt ───────────────────────────────────────────────────

    @Nested
    @DisplayName("recordFailedAttempt Tests")
    class RecordFailedAttemptTests {

        @Test
        @DisplayName("Increments the attempt counter on each call")
        void recordFailedAttempt_IncrementsCounter() {
            service.recordFailedAttempt("user");
            service.recordFailedAttempt("user");

            Object record = getAttempts().get("user");
            assertNotNull(record);
            assertEquals(2, (int) ReflectionTestUtils.getField(record, "count"));
        }

        @Test
        @DisplayName("Does not set lockoutUntil below the threshold")
        void recordFailedAttempt_NoLockoutBelowThreshold() {
            for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS - 1; i++) {
                service.recordFailedAttempt("user");
            }
            Object record = getAttempts().get("user");
            assertNull(ReflectionTestUtils.getField(record, "lockoutUntil"));
        }

        @Test
        @DisplayName("Sets lockoutUntil when reaching MAX_ATTEMPTS")
        void recordFailedAttempt_SetsLockoutAtMaxAttempts() {
            for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
                service.recordFailedAttempt("user");
            }
            Object record = getAttempts().get("user");
            LocalDateTime lockoutUntil = (LocalDateTime) ReflectionTestUtils.getField(record, "lockoutUntil");
            assertNotNull(lockoutUntil);
            assertTrue(lockoutUntil.isAfter(LocalDateTime.now()));
        }

        @Test
        @DisplayName("Tracks separate attempt counters per credential")
        void recordFailedAttempt_IndependentPerCredential() {
            for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
                service.recordFailedAttempt("user1");
            }
            service.recordFailedAttempt("user2");

            assertTrue(service.isLocked("user1"));
            assertFalse(service.isLocked("user2"));
        }
    }

    // ── resetAttempts ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("resetAttempts Tests")
    class ResetAttemptsTests {

        @Test
        @DisplayName("Removes the attempt record")
        void resetAttempts_RemovesRecord() {
            service.recordFailedAttempt("user");
            service.resetAttempts("user");

            assertFalse(getAttempts().containsKey("user"));
        }

        @Test
        @DisplayName("Clears a lockout")
        void resetAttempts_ClearsLockout() {
            for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
                service.recordFailedAttempt("user");
            }
            assertTrue(service.isLocked("user"));

            service.resetAttempts("user");
            assertFalse(service.isLocked("user"));
        }

        @Test
        @DisplayName("Does not throw when credential has no record")
        void resetAttempts_NonExistentKeyIsNoop() {
            assertDoesNotThrow(() -> service.resetAttempts("unknown"));
        }

        @Test
        @DisplayName("Is case-insensitive")
        void resetAttempts_CaseInsensitive() {
            for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
                service.recordFailedAttempt("USER");
            }
            service.resetAttempts("user");

            assertFalse(service.isLocked("USER"));
        }
    }

    // ── cleanupExpiredLockouts ────────────────────────────────────────────────

    @Nested
    @DisplayName("cleanupExpiredLockouts Tests")
    class CleanupExpiredLockoutsTests {

        @Test
        @DisplayName("Removes entries whose lockout has expired")
        void cleanup_RemovesExpiredEntries() {
            for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
                service.recordFailedAttempt("user");
            }
            setLockoutUntil("user", LocalDateTime.now().minusMinutes(2));

            service.cleanupExpiredLockouts();

            assertFalse(getAttempts().containsKey("user"));
        }

        @Test
        @DisplayName("Keeps entries whose lockout is still active")
        void cleanup_KeepsActiveEntries() {
            for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
                service.recordFailedAttempt("user");
            }

            service.cleanupExpiredLockouts();

            assertTrue(getAttempts().containsKey("user"));
        }

        @Test
        @DisplayName("Ignores records that have no lockout set")
        void cleanup_IgnoresRecordsWithoutLockout() {
            service.recordFailedAttempt("user"); // count=1, lockoutUntil=null

            service.cleanupExpiredLockouts();

            assertTrue(getAttempts().containsKey("user"));
        }

        @Test
        @DisplayName("Handles an empty map gracefully")
        void cleanup_EmptyMapIsNoop() {
            assertDoesNotThrow(() -> service.cleanupExpiredLockouts());
        }

        @Test
        @DisplayName("Only removes expired entries when mixed")
        void cleanup_OnlyRemovesExpiredWhenMixed() {
            // user1: expired lockout
            for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
                service.recordFailedAttempt("user1");
            }
            setLockoutUntil("user1", LocalDateTime.now().minusMinutes(2));

            // user2: active lockout
            for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
                service.recordFailedAttempt("user2");
            }

            service.cleanupExpiredLockouts();

            assertFalse(getAttempts().containsKey("user1"));
            assertTrue(getAttempts().containsKey("user2"));
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<String, Object> getAttempts() {
        return (ConcurrentHashMap<String, Object>) ReflectionTestUtils.getField(service, "attempts");
    }

    private void setLockoutUntil(String credential, LocalDateTime time) {
        Object record = getAttempts().get(credential.toLowerCase());
        if (record != null) {
            ReflectionTestUtils.setField(record, "lockoutUntil", time);
        }
    }
}
