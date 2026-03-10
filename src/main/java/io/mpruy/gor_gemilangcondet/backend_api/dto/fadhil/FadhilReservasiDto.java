package io.mpruy.gor_gemilangcondet.backend_api.dto.fadhil;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data reservasi dari endpoint GET /bookings Fadhil.
 * Hanya field yang dibutuhkan untuk keperluan tampilan jadwal real-time.
 *
 * <p>Digunakan oleh admin/staf untuk menampilkan {@code namaWakil}
 * pada slot yang terisi di grid jadwal.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FadhilReservasiDto {

    private UUID id;

    /** UUID lapangan yang dipesan */
    private UUID lapanganId;

    private String lapanganName;

    /** Nama pemesan / perwakilan yang dipesan — RBAC: hanya terlihat oleh admin/staf */
    private String namaWakil;

    /** Waktu mulai reservasi (bisa span lebih dari 1 jam: durationInHours 1–5) */
    private LocalDateTime reservationStart;

    /** Waktu selesai reservasi */
    private LocalDateTime reservationEnd;

    /**
     * Status reservasi: BELUM_DIBAYAR, MENUNGGU_KONFIRMASI_STAF,
     * DIKONFIRMASI, DITOLAK, EXPIRED, DOWN_PAYMENT, DIBAYAR, DIBATALKAN, SELESAI
     */
    private String status;
}
