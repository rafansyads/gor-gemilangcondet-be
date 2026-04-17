package io.mpruy.gor_gemilangcondet.backend_api.config.scheduler;

import io.mpruy.gor_gemilangcondet.backend_api.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class UserScheduler {

    private final UserService userService;

    @Scheduled(fixedRate = 60_000) // every 1 minute
    public void deleteExpiredBannedUsers() {
        int deleted = userService.deleteExpiredBannedUsers();
        if (deleted > 0) {
            log.info("UserScheduler: deleted {} banned user(s) older than 3 days", deleted);
        }
    }
}
