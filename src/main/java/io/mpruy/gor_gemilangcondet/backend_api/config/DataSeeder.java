package io.mpruy.gor_gemilangcondet.backend_api.config;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
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
import io.mpruy.gor_gemilangcondet.backend_api.repository.LapanganRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.AlatOlahragaRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;

/**
 * Ensures every {@link RoleName} value has a corresponding row in the
 * {@code roles}
 * table before any request is handled. Safe to run on every startup — existing
 * rows
 * are left untouched.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final LapanganRepository lapanganRepository;
    private final AlatOlahragaRepository alatOlahragaRepository;
    private final EntityManager entityManager;

    @Override
    public void run(ApplicationArguments args) {
        seedRoles();
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
                Lapangan.builder().name("Badminton 1").kode("BDM-001").type(LapanganType.BADMINTON)
                        .jenisLantai("Vinyl").fasilitas(List.of("LED Lighting", "Fan", "Vinyl Flooring"))
                        .status(LapanganStatus.TERSEDIA).tarifPerJam(50000).createdAt(now)
                        .updatedAt(now).build(),
                Lapangan.builder().name("Badminton 2").kode("BDM-002").type(LapanganType.BADMINTON)
                        .jenisLantai("Vinyl").fasilitas(List.of("LED Lighting", "Fan", "Vinyl Flooring"))
                        .status(LapanganStatus.TERSEDIA).tarifPerJam(50000).createdAt(now)
                        .updatedAt(now).build(),
                Lapangan.builder().name("Badminton 3").kode("BDM-003").type(LapanganType.BADMINTON)
                        .jenisLantai("Vinyl").fasilitas(List.of("LED Lighting", "Fan", "Vinyl Flooring"))
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
     * Uses JOINED inheritance — AlatOlahraga extends Barang. The unique constraint
     * on 'name' lives in the parent 'barang' table. On schema changes the join
     * table may be empty while 'barang' still holds orphaned rows.
     * Fix: delete orphaned barang rows first via native query, then re-seed.
     */
    @Transactional
    public void seedAlatOlahraga() {
        // Delete orphaned barang rows (no matching alat_olahraga child) first
        try {
            int deleted = entityManager
                    .createNativeQuery(
                            "DELETE FROM barang WHERE id NOT IN (SELECT id FROM alat_olahraga)")
                    .executeUpdate();
            if (deleted > 0) {
                log.info("Cleaned up {} orphaned barang row(s)", deleted);
            }
            entityManager.flush();
        } catch (Exception ex) {
            log.warn("Could not clean orphaned barang rows: {}", ex.getMessage());
        }

        // If alat_olahraga already has rows, stop
        if (alatOlahragaRepository.count() > 0) return;

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
