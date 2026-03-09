package io.mpruy.gor_gemilangcondet.backend_api.controller;

import io.mpruy.gor_gemilangcondet.backend_api.dto.response.ScheduleResponse;
import io.mpruy.gor_gemilangcondet.backend_api.security.JwtRoleExtractor;
import io.mpruy.gor_gemilangcondet.backend_api.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST endpoint untuk fitur Jadwal Real-Time.
 *
 * <h3>Endpoint</h3>
 * <pre>
 * GET /api/schedule?date=yyyy-MM-dd
 * </pre>
 *
 * <p>Mengembalikan grid jadwal lengkap (lapangan × jam) untuk tanggal yang diminta.
 * Jika parameter {@code date} tidak diberikan, default ke hari ini.
 *
 * <p>Endpoint ini <strong>publik</strong> (tidak perlu login) sehingga pelanggan
 * dapat melihat ketersediaan lapangan tanpa registrasi.
 */
@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // FE development; ganti dengan origins spesifik di production
public class ScheduleController {

    private final ScheduleService     scheduleService;
    private final JwtRoleExtractor    jwtRoleExtractor;

    /**
     * Ambil jadwal lapangan untuk tanggal tertentu.
     *
     * <p>Data diambil dari backend Fadhil. Role yang terdeteksi dari JWT
     * menentukan apakah {@code namaWakil} akan disertakan di respons.
     *
     * @param authHeader header Authorization (opsional, format: Bearer &lt;token&gt;)
     * @param date       tanggal yang diminta (format: {@code yyyy-MM-dd}), default hari ini
     * @return {@link ScheduleResponse} berisi grid waktu × lapangan
     */
    @GetMapping
    public ResponseEntity<ScheduleResponse> getSchedule(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        String role = jwtRoleExtractor.extractRole(authHeader);
        return ResponseEntity.ok(scheduleService.getScheduleFromFadhil(targetDate, role));
    }
}
