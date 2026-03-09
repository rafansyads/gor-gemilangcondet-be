package io.mpruy.gor_gemilangcondet.backend_api.config;

import io.mpruy.gor_gemilangcondet.backend_api.entity.Court;
import io.mpruy.gor_gemilangcondet.backend_api.repository.CourtRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Seed data awal aplikasi.
 *
 * <p>Memastikan 6 lapangan GOR Gemilang Condet sudah tercatat di database
 * sebelum aplikasi siap menerima request. Aman dijalankan berulang kali
 * (idempoten) karena hanya insert jika lapangan belum ada.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final int TOTAL_COURTS = 6;

    private final CourtRepository courtRepository;

    @Override
    public void run(ApplicationArguments args) {
        int seeded = 0;
        for (int i = 1; i <= TOTAL_COURTS; i++) {
            int courtId = i;
            if (!courtRepository.existsById(courtId)) {
                courtRepository.save(Court.builder()
                        .id(courtId)
                        .name("Court " + courtId)
                        .build());
                seeded++;
            }
        }

        if (seeded > 0) {
            log.info("[INIT] Berhasil menyimpan {} lapangan baru ke database.", seeded);
        } else {
            log.info("[INIT] Semua lapangan sudah ada di database, tidak perlu seed ulang.");
        }
    }
}
