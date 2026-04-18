package io.mpruy.gor_gemilangcondet.backend_api.config;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.EntityManager;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.Lapangan;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.LapanganStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.LapanganType;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.Barang;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.alat_olahraga.AlatOlahraga;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.alat_olahraga.AlatOlahragaStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.alat_olahraga.AlatOlahragaType;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.kantin.BarangKantin;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.kantin.BarangKantinStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.kantin.BarangKantinType;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.toko.BarangToko;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.toko.BarangTokoStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.toko.BarangTokoType;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.Role;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.RoleName;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatusName;
import io.mpruy.gor_gemilangcondet.backend_api.repository.BarangRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.BarangKantinRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.BarangTokoRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.LapanganRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.AlatOlahragaRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.RoleRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserStatusRepository;
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
    private final UserStatusRepository userStatusRepository;
    private final LapanganRepository lapanganRepository;
    private final AlatOlahragaRepository alatOlahragaRepository;
    private final BarangRepository barangRepository;
    private final BarangKantinRepository barangKantinRepository;
    private final BarangTokoRepository barangTokoRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedUserStatuses();
        seedRoles();
        seedLapangan(); // nantinya tergantung GOR
        seedAlatOlahraga(); // nantinya tergantung GOR, bisa jadi tidak ada alat olahraga yang disewakan
        migrateLegacySellableRowsToConcreteSubclasses();
        seedBarangJual(); // seed makanan & minuman untuk dijual di kasir
        resetAllLapanganToTersedia();
    }

    @Transactional
    private void migrateLegacySellableRowsToConcreteSubclasses() {
        // Convert legacy plain barang rows (pre-abstract migration) into concrete
        // child rows while preserving existing IDs and FK references.
        migrateLegacyKantin("Nasi Goreng", "MAKANAN_BERAT", 10);
        migrateLegacyKantin("Mie Goreng", "MAKANAN_BERAT", 10);
        migrateLegacyKantin("Roti Bakar", "MAKANAN_RINGAN", 8);
        migrateLegacyKantin("Kentang Goreng", "MAKANAN_RINGAN", 8);
        migrateLegacyKantin("Teh Botol", "MINUMAN", 20);
        migrateLegacyKantin("Air Mineral", "MINUMAN", 20);
        migrateLegacyKantin("Kopi Hitam", "MINUMAN", 15);
        migrateLegacyKantin("Es Jeruk", "MINUMAN", 15);
        migrateLegacyKantin("Pocari Sweat", "MINUMAN", 20);

        migrateLegacyToko("Raket Yonex", "ALAT_OLAHRAGA");
        migrateLegacyToko("Raket Li-Ning", "ALAT_OLAHRAGA");

        entityManager.flush();
    }

    private void migrateLegacyKantin(String name, String type, long threshold) {
        entityManager.createNativeQuery(
                "INSERT INTO barang_kantin (id, type, status, reorder_threshold) "
                        + "SELECT b.id, :type, CASE WHEN b.stock > 0 THEN 'TERSEDIA' ELSE 'TERJUAL' END, :threshold "
                        + "FROM barang b "
                        + "LEFT JOIN barang_kantin bk ON bk.id = b.id "
                        + "LEFT JOIN barang_toko bt ON bt.id = b.id "
                        + "LEFT JOIN alat_olahraga ao ON ao.id = b.id "
                        + "WHERE b.name = :name AND bk.id IS NULL AND bt.id IS NULL AND ao.id IS NULL")
                .setParameter("name", name)
                .setParameter("type", type)
                .setParameter("threshold", threshold)
                .executeUpdate();
    }

    private void migrateLegacyToko(String name, String type) {
        entityManager.createNativeQuery(
                "INSERT INTO barang_toko (id, type, status) "
                        + "SELECT b.id, :type, CASE WHEN b.stock > 0 THEN 'TERSEDIA' ELSE 'TERJUAL' END "
                        + "FROM barang b "
                        + "LEFT JOIN barang_kantin bk ON bk.id = b.id "
                        + "LEFT JOIN barang_toko bt ON bt.id = b.id "
                        + "LEFT JOIN alat_olahraga ao ON ao.id = b.id "
                        + "WHERE b.name = :name AND bk.id IS NULL AND bt.id IS NULL AND ao.id IS NULL")
                .setParameter("name", name)
                .setParameter("type", type)
                .executeUpdate();
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

    private void seedUserStatuses() {
        for (UserStatusName statusName : UserStatusName.values()) {
            if (userStatusRepository.findByName(statusName).isEmpty()) {
                userStatusRepository.save(UserStatus.builder().name(statusName).build());
                log.info("Seeded user status: {}", statusName);
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
     * @see AlatOlahragaType
     * @see AlatOlahragaStatus
     * @see AlatOlahragaRepository
     */
    private void seedAlatOlahragaTypes() {
        for (AlatOlahragaType type : AlatOlahragaType.values()) {
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
        // Clean up orphaned barang rows that were meant to be alat_olahraga
        // but lost their child row due to schema changes. Only deletes barang
        // that have a matching name in the seed list but no alat_olahraga child.
        try {
            int deleted = entityManager
                    .createNativeQuery(
                            "DELETE FROM barang b WHERE b.id NOT IN (SELECT ao.id FROM alat_olahraga ao) "
                                    + "AND b.name = 'Raket Badminton Premium'")
                    .executeUpdate();
            if (deleted > 0) {
                log.info("Cleaned up {} orphaned barang row(s)", deleted);
            }
            entityManager.flush();
        } catch (Exception ex) {
            log.warn("Could not clean orphaned barang rows: {}", ex.getMessage());
        }

        // If alat_olahraga already has rows, stop
        if (alatOlahragaRepository.count() > 0)
            return;

        LocalDateTime now = LocalDateTime.now();
        List<AlatOlahraga> equipment = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            equipment.add(
                    AlatOlahraga.builder()
                            .name(String.format("Raket Badminton Premium #%02d", i))
                            .type(AlatOlahragaType.RAKET)
                            .stock(1)
                            .price(25000)
                            .status(AlatOlahragaStatus.TERSEDIA)
                            .createdAt(now)
                            .updatedAt(now)
                            .build());
        }

        alatOlahragaRepository.saveAll(equipment);
        log.info("Seeded {} unit alat olahraga tipe RAKET", equipment.size());
    }

    /**
     * Development-seeding.
     * Seeds sellable items into concrete subclasses (kantin & toko).
     * Idempotent by name to avoid duplicate seeds on repeated startup.
     */
    @Transactional
    public void seedBarangJual() {
        Set<String> existingNames = new HashSet<>(
                barangRepository.findAll().stream()
                        .map(Barang::getName)
                        .toList());

        List<String> targetNames = List.of(
                "Nasi Goreng", "Mie Goreng", "Roti Bakar", "Kentang Goreng",
                "Raket Yonex", "Raket Li-Ning",
                "Teh Botol", "Air Mineral", "Kopi Hitam", "Es Jeruk", "Pocari Sweat");

        boolean allSeeded = targetNames.stream().allMatch(existingNames::contains);
        if (allSeeded) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        List<BarangKantin> kantinItems = new ArrayList<>();
        List<BarangToko> tokoItems = new ArrayList<>();

        if (!existingNames.contains("Nasi Goreng")) {
            kantinItems.add(BarangKantin.builder().name("Nasi Goreng")
                    .type(BarangKantinType.MAKANAN_BERAT).status(BarangKantinStatus.TERSEDIA)
                    .reorderThreshold(10).stock(50).price(15000).createdAt(now).updatedAt(now).build());
        }
        if (!existingNames.contains("Mie Goreng")) {
            kantinItems.add(BarangKantin.builder().name("Mie Goreng")
                    .type(BarangKantinType.MAKANAN_BERAT).status(BarangKantinStatus.TERSEDIA)
                    .reorderThreshold(10).stock(50).price(12000).createdAt(now).updatedAt(now).build());
        }
        if (!existingNames.contains("Roti Bakar")) {
            kantinItems.add(BarangKantin.builder().name("Roti Bakar")
                    .type(BarangKantinType.MAKANAN_RINGAN).status(BarangKantinStatus.TERSEDIA)
                    .reorderThreshold(8).stock(30).price(10000).createdAt(now).updatedAt(now).build());
        }
        if (!existingNames.contains("Kentang Goreng")) {
            kantinItems.add(BarangKantin.builder().name("Kentang Goreng")
                    .type(BarangKantinType.MAKANAN_RINGAN).status(BarangKantinStatus.TERSEDIA)
                    .reorderThreshold(8).stock(40).price(12000).createdAt(now).updatedAt(now).build());
        }

        if (!existingNames.contains("Teh Botol")) {
            kantinItems.add(BarangKantin.builder().name("Teh Botol")
                    .type(BarangKantinType.MINUMAN).status(BarangKantinStatus.TERSEDIA)
                    .reorderThreshold(20).stock(100).price(7000).createdAt(now).updatedAt(now).build());
        }
        if (!existingNames.contains("Air Mineral")) {
            kantinItems.add(BarangKantin.builder().name("Air Mineral")
                    .type(BarangKantinType.MINUMAN).status(BarangKantinStatus.TERSEDIA)
                    .reorderThreshold(20).stock(100).price(5000).createdAt(now).updatedAt(now).build());
        }
        if (!existingNames.contains("Kopi Hitam")) {
            kantinItems.add(BarangKantin.builder().name("Kopi Hitam")
                    .type(BarangKantinType.MINUMAN).status(BarangKantinStatus.TERSEDIA)
                    .reorderThreshold(15).stock(60).price(10000).createdAt(now).updatedAt(now).build());
        }
        if (!existingNames.contains("Es Jeruk")) {
            kantinItems.add(BarangKantin.builder().name("Es Jeruk")
                    .type(BarangKantinType.MINUMAN).status(BarangKantinStatus.TERSEDIA)
                    .reorderThreshold(15).stock(60).price(8000).createdAt(now).updatedAt(now).build());
        }
        if (!existingNames.contains("Pocari Sweat")) {
            kantinItems.add(BarangKantin.builder().name("Pocari Sweat")
                    .type(BarangKantinType.MINUMAN).status(BarangKantinStatus.TERSEDIA)
                    .reorderThreshold(20).stock(80).price(10000).createdAt(now).updatedAt(now).build());
        }

        if (!existingNames.contains("Raket Yonex")) {
            tokoItems.add(BarangToko.builder().name("Raket Yonex")
                    .type(BarangTokoType.ALAT_OLAHRAGA).status(BarangTokoStatus.TERSEDIA)
                    .stock(10).price(450000).createdAt(now).updatedAt(now).build());
        }
        if (!existingNames.contains("Raket Li-Ning")) {
            tokoItems.add(BarangToko.builder().name("Raket Li-Ning")
                    .type(BarangTokoType.ALAT_OLAHRAGA).status(BarangTokoStatus.TERSEDIA)
                    .stock(8).price(350000).createdAt(now).updatedAt(now).build());
        }

        if (!kantinItems.isEmpty()) {
            barangKantinRepository.saveAll(kantinItems);
            kantinItems.forEach(b -> log.info("Seeded barang kantin: {} - stok: {} - harga: {}",
                    b.getName(), b.getStock(), b.getPrice()));
        }

        if (!tokoItems.isEmpty()) {
            barangTokoRepository.saveAll(tokoItems);
            tokoItems.forEach(b -> log.info("Seeded barang toko: {} - stok: {} - harga: {}",
                    b.getName(), b.getStock(), b.getPrice()));
        }
    }
}
