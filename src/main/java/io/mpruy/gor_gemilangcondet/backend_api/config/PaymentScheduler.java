package io.mpruy.gor_gemilangcondet.backend_api.config;

import io.mpruy.gor_gemilangcondet.backend_api.service.PembayaranService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically checks and expires reservations that exceed their payment
 * deadline (10 minutes). Runs every 30 seconds.
 */
@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class PaymentScheduler {

    private final PembayaranService pembayaranService;

    @Scheduled(fixedRate = 30_000) // every 30 seconds
    public void expireOverdueReservations() {
        int count = pembayaranService.expireOverdueReservations();
        if (count > 0) {
            log.info("PaymentScheduler: expired {} overdue reservation(s)", count);
        }
    }
}
