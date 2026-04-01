package io.mpruy.gor_gemilangcondet.backend_api.config;

import io.mpruy.gor_gemilangcondet.backend_api.service.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically cleans up expired login lockouts tracked by
 * {@link LoginAttemptService}. Runs every minute.
 */
@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class AuthScheduler {

    private final LoginAttemptService loginAttemptService;

    @Scheduled(fixedRate = 60_000) // every 1 minute
    public void cleanupExpiredLoginLockouts() {
        loginAttemptService.cleanupExpiredLockouts();
    }
}
