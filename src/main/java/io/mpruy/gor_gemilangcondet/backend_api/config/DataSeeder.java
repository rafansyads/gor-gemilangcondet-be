package io.mpruy.gor_gemilangcondet.backend_api.config;

import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.Lapangan;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.LapanganStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.LapanganType;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.Role;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.RoleName;
import io.mpruy.gor_gemilangcondet.backend_api.repository.LapanganRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Ensures every {@link RoleName} value has a corresponding row in the {@code roles}
 * table before any request is handled. Safe to run on every startup — existing rows
 * are left untouched.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final LapanganRepository lapanganRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedRoles();
        seedLapangan();
    }

    private void seedRoles() {
        for (RoleName roleName : RoleName.values()) {
            if (roleRepository.findByRoleName(roleName).isEmpty()) {
                roleRepository.save(Role.builder().roleName(roleName).build());
                log.info("Seeded role: {}", roleName);
            }
        }
    }

    private void seedLapangan() {
        if (lapanganRepository.count() > 0) return;

        LocalDateTime now = LocalDateTime.now();
        List<Lapangan> courts = List.of(
                Lapangan.builder().name("Badminton 1").type(LapanganType.BADMINTON)
                        .status(LapanganStatus.TERSEDIA).tarifPerJam(50000).createdAt(now).updatedAt(now).build(),
                Lapangan.builder().name("Badminton 2").type(LapanganType.BADMINTON)
                        .status(LapanganStatus.TERSEDIA).tarifPerJam(50000).createdAt(now).updatedAt(now).build(),
                Lapangan.builder().name("Futsal 1").type(LapanganType.FUTSAL)
                        .status(LapanganStatus.TERSEDIA).tarifPerJam(200000).createdAt(now).updatedAt(now).build(),
                Lapangan.builder().name("Basket 1").type(LapanganType.BASKET)
                        .status(LapanganStatus.TERSEDIA).tarifPerJam(150000).createdAt(now).updatedAt(now).build(),
                Lapangan.builder().name("Voli 1").type(LapanganType.VOLI)
                        .status(LapanganStatus.TERSEDIA).tarifPerJam(100000).createdAt(now).updatedAt(now).build()
        );

        lapanganRepository.saveAll(courts);
        courts.forEach(c -> log.info("Seeded lapangan: {} ({})", c.getName(), c.getType()));
    }
}
