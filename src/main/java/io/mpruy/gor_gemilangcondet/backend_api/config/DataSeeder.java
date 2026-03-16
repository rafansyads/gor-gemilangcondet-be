package io.mpruy.gor_gemilangcondet.backend_api.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.Lapangan;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.LapanganStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.LapanganType;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.AlatOlahraga;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.AlatOlahragaStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.BarangType;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.Role;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.RoleName;
import io.mpruy.gor_gemilangcondet.backend_api.entity.Court;
import io.mpruy.gor_gemilangcondet.backend_api.repository.AlatOlahragaRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.CourtRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.LapanganRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Ensures every {@link RoleName} value has a corresponding row in the
 * {@code roles}
 * table before any request is handled. Safe to run on every startup — existing
 * rows
 * are left untouched.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final LapanganRepository lapanganRepository;
    private final AlatOlahragaRepository alatOlahragaRepository;
    private final CourtRepository courtRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedRoles();
        seedCourts();
        seedLapangan(); // nantinya tergantung GOR
        seedAlatOlahraga(); // nantinya tergantung GOR, bisa jadi tidak ada alat olahraga yang disewakan
        resetAllLapanganToTersedia();
    }

    /**
     * Memastikan setiap nilai enum {@link RoleName} memiliki baris yang sesuai di
     * tabel
     * {@code roles} sebelum permintaan apapun diproses. Aman dijalankan setiap
     * startup
     * — baris yang sudah ada tidak akan diubah.
     *
     * @see RoleName
     * @see RoleRepository
     */
    private void seedRoles() {
        for (RoleName roleName : RoleName.values()) {
            if (roleRepository.findByRoleName(roleName).isEmpty()) {
                roleRepository.save(Role.builder().roleName(roleName).build());
                log.info("Seeded role: {}", roleName);
            }
        }
    }

    /**
     * Menyiapkan 6 lapangan (court) yang merepresentasikan lapangan badminton
     * di GOR Gemilang Condet (id 1–6, nama "Court 1" s/d "Court 6").
     *
     * <p>
     * Metode ini idempotent — jika data sudah ada, tidak akan membuat duplikasi.
     *
     * @see Court
     * @see CourtRepository
     */
    private void seedCourts() {
        for (int i = 1; i <= 6; i++) {
            final int id = i;
            if (courtRepository.findById(id).isEmpty()) {
                Court court = Court.builder()
                        .id(id)
                        .name("Court " + id)
                        .build();
                courtRepository.save(court);
                log.info("Seeded court: {} (id={})", court.getName(), court.getId());
            }
        }
    }

    /**
     * Development-seeding.
     * Checks each Lapangan Type against the database and inserts any missing
     * lapangan with default values.
     * This method is idempotent and can be safely run on every application startup
     * without creating duplicate entries. It ensures that the application always
     * has the necessary lapangan defined for proper reservation handling.
     * 
     * @see LapanganType
     * @see LapanganStatus
     * @see LapanganRepository
     */
    private void seedLapanganTypes() {
        for (LapanganType type : LapanganType.values()) {
            if (lapanganRepository.findByType(type).isEmpty()) {
                LocalDateTime now = LocalDateTime.now();
                Lapangan lapangan = Lapangan.builder()
                        .name(type.name() + " Court")
                        .type(type)
                        .status(LapanganStatus.TERSEDIA)
                        .tarifPerJam(100000)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();
                lapanganRepository.save(lapangan);
                log.info("Seeded lapangan: {} ({})", lapangan.getName(), lapangan.getType());
            }
        }
    }

    /**
     * Development-seeding.
     * Checks each AlatOlahraga Type against the database and inserts any missing
     * equipment with default values.
     * This method is idempotent and can be safely run on every application startup
     * without creating duplicate entries. It ensures that the application always
     * has the necessary equipment defined for proper reservation handling.
     * 
     * @see BarangType
     * @see AlatOlahragaStatus
     * @see AlatOlahragaRepository
     */
    private void seedAlatOlahragaTypes() {
        for (BarangType type : BarangType.values()) {
            if (alatOlahragaRepository.findByTypeIn(List.of(type)).isEmpty()) {
                LocalDateTime now = LocalDateTime.now();
                AlatOlahraga alat = AlatOlahraga.builder()
                        .name(type.name() + " Item")
                        .type(type)
                        .stock(10)
                        .price(5000)
                        .status(AlatOlahragaStatus.TERSEDIA)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();
                alatOlahragaRepository.save(alat);
                log.info("Seeded alat olahraga: {} ({}) - stok: {}",
                        alat.getName(), alat.getType(), alat.getStock());
            }
        }
    }

    /**
     * Development-seeding.
     */
    private void seedLapangan() {
        if (lapanganRepository.count() > 0)
            return;

        LocalDateTime now = LocalDateTime.now();
        List<Lapangan> courts = List.of(
                Lapangan.builder().name("Badminton 1").type(LapanganType.BADMINTON)
                        .status(LapanganStatus.TERSEDIA).tarifPerJam(50000).createdAt(now)
                        .updatedAt(now).build(),
                Lapangan.builder().name("Badminton 2").type(LapanganType.BADMINTON)
                        .status(LapanganStatus.TERSEDIA).tarifPerJam(50000).createdAt(now)
                        .updatedAt(now).build(),
                Lapangan.builder().name("Badminton 3").type(LapanganType.BADMINTON)
                        .status(LapanganStatus.TERSEDIA).tarifPerJam(150000).createdAt(now)
                        .updatedAt(now).build(),
                Lapangan.builder().name("Badminton 4").type(LapanganType.BADMINTON)
                        .status(LapanganStatus.TERSEDIA).tarifPerJam(150000).createdAt(now)
                        .updatedAt(now).build(),
                Lapangan.builder().name("Badminton 5").type(LapanganType.BADMINTON)
                        .status(LapanganStatus.TERSEDIA).tarifPerJam(150000).createdAt(now)
                        .updatedAt(now).build(),
                Lapangan.builder().name("Badminton 6").type(LapanganType.BADMINTON)
                        .status(LapanganStatus.TERSEDIA).tarifPerJam(150000).createdAt(now)
                        .updatedAt(now).build());

        lapanganRepository.saveAll(courts);
        courts.forEach(c -> log.info("Seeded lapangan: {} ({})", c.getName(), c.getType()));
    }

    /**
     * Reset semua lapangan yang tidak TERSEDIA kembali ke TERSEDIA saat startup.
     * Berguna di development agar semua lapangan selalu bisa dipesan ulang.
     */
    private void resetAllLapanganToTersedia() {
        LocalDateTime now = LocalDateTime.now();
        lapanganRepository.findAll().forEach(lapangan -> {
            if (lapangan.getStatus() != LapanganStatus.TERSEDIA) {
                lapangan.setStatus(LapanganStatus.TERSEDIA);
                lapangan.setMaintenanceStart(null);
                lapangan.setMaintenanceEnd(null);
                lapangan.setUpdatedAt(now);
                lapanganRepository.save(lapangan);
                log.info("Reset lapangan ke TERSEDIA: {} ({})", lapangan.getName(), lapangan.getType());
            }
        });
    }

    /**
     * Development-seeding.
     */
    private void seedAlatOlahraga() {
        if (alatOlahragaRepository.count() > 0)
            return;

        LocalDateTime now = LocalDateTime.now();
        List<AlatOlahraga> equipment = List.of(
                AlatOlahraga.builder().name("Raket Badminton Premium").type(BarangType.RAKET)
                        .stock(20).price(25000).status(AlatOlahragaStatus.TERSEDIA)
                        .createdAt(now).updatedAt(now).build());

        alatOlahragaRepository.saveAll(equipment);
        equipment.forEach(e -> log.info("Seeded alat olahraga: {} ({}) - stok: {}",
                e.getName(), e.getType(), e.getStock()));
    }
}
