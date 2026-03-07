package io.mpruy.gor_gemilangcondet.backend_api.controller;

import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseRequestDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseResponseDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.requests.CreateLapanganRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.requests.UpdateLapanganRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.LapanganResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.LapanganStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.LapanganType;
import io.mpruy.gor_gemilangcondet.backend_api.service.ReservasiService;
import io.mpruy.gor_gemilangcondet.backend_api.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/courts")
@RequiredArgsConstructor
public class LapanganController {

    private final ReservasiService reservasiService;

    // ──────────────────────────────────────────────────────────────────────────
    // GET /courts — List all courts (optionally filtered by type)
    // ──────────────────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<BaseResponseDto<List<LapanganResponse>>> getCourts(
            @RequestParam(required = false) LapanganType type) {
        try {
            List<LapanganResponse> courts = (type != null)
                    ? reservasiService.getCourtsByType(type)
                    : reservasiService.getAllCourts();
            return ResponseUtil.success(courts, "Data lapangan berhasil diambil", HttpStatus.OK)
                    .toBuilder().build();
        } catch (Exception ex) {
            return ResponseUtil.error(
                    "Gagal mengambil data lapangan: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /courts — Create a new court
    // ──────────────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<BaseResponseDto<LapanganResponse>> createCourt(
            @Validated @RequestBody BaseRequestDto<CreateLapanganRequest> request) {
        try {
            LapanganResponse response = reservasiService.createCourt(request.getData());
            return ResponseUtil.success(response, "Lapangan berhasil ditambahkan", HttpStatus.CREATED)
                    .toBuilder().build();
        } catch (IllegalArgumentException ex) {
            return ResponseUtil.error(ex.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception ex) {
            return ResponseUtil.error(
                    "Gagal menambahkan lapangan: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PUT /courts/{id} — Update court
    // ──────────────────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    public ResponseEntity<BaseResponseDto<LapanganResponse>> updateCourt(
            @PathVariable UUID id,
            @Validated @RequestBody BaseRequestDto<UpdateLapanganRequest> request) {
        try {
            LapanganResponse response = reservasiService.updateCourt(id, request.getData());
            return ResponseUtil.success(response, "Lapangan berhasil diperbarui", HttpStatus.OK)
                    .toBuilder().build();
        } catch (IllegalArgumentException ex) {
            return ResponseUtil.error(ex.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            return ResponseUtil.error(
                    "Gagal memperbarui lapangan: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // DELETE /courts/{id} — Delete court
    // ──────────────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponseDto<Object>> deleteCourt(@PathVariable UUID id) {
        try {
            reservasiService.deleteCourt(id);
            return ResponseUtil.success(null, "Lapangan berhasil dihapus", HttpStatus.OK)
                    .toBuilder().build();
        } catch (IllegalArgumentException ex) {
            return ResponseUtil.error(ex.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception ex) {
            return ResponseUtil.error(
                    "Gagal menghapus lapangan: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PATCH /courts/{id}/status — Activate / Deactivate court
    // ──────────────────────────────────────────────────────────────────────────

    @PatchMapping("/{id}/status")
    public ResponseEntity<BaseResponseDto<LapanganResponse>> updateCourtStatus(
            @PathVariable UUID id,
            @RequestParam LapanganStatus status) {
        try {
            LapanganResponse response = reservasiService.updateCourtStatus(id, status);
            return ResponseUtil.success(response, "Status lapangan berhasil diperbarui", HttpStatus.OK)
                    .toBuilder().build();
        } catch (IllegalArgumentException ex) {
            return ResponseUtil.error(ex.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            return ResponseUtil.error(
                    "Gagal memperbarui status lapangan: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
