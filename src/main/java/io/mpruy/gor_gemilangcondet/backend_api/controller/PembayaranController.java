package io.mpruy.gor_gemilangcondet.backend_api.controller;

import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseResponseDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.ConfirmPaymentResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.PembayaranResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.ReservasiResponse;
import io.mpruy.gor_gemilangcondet.backend_api.service.PembayaranService;
import io.mpruy.gor_gemilangcondet.backend_api.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PembayaranController {

    private final PembayaranService pembayaranService;

    @Value("${app.upload.dir:uploads/payment-proofs}")
    private String uploadDir;

    // ──────────────────────────────────────────────────────────────────────────
    // GET /payments/unpaid — List all unpaid reservations (Staff)
    // ──────────────────────────────────────────────────────────────────────────

    @GetMapping("/unpaid")
    @PreAuthorize("hasAnyAuthority('STAF_LAPANGAN', 'STAF_TOKO', 'OWNER', 'ADMIN')")
    public ResponseEntity<BaseResponseDto<List<ReservasiResponse>>> getUnpaidReservations() {
        List<ReservasiResponse> unpaid = pembayaranService.getUnpaidReservations();
        return ResponseUtil.success(unpaid,
                "Data reservasi belum dibayar berhasil diambil", HttpStatus.OK)
                .toBuilder().build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /payments/staff-reservations — Reservations for staff dashboard
    // ──────────────────────────────────────────────────────────────────────────

    @GetMapping("/staff-reservations")
    @PreAuthorize("hasAnyAuthority('STAF_LAPANGAN', 'STAF_TOKO', 'OWNER', 'ADMIN')")
    public ResponseEntity<BaseResponseDto<List<ReservasiResponse>>> getStaffReservations() {
        List<ReservasiResponse> reservations = pembayaranService.getReservationsForStaff();
        return ResponseUtil.success(reservations,
                "Data reservasi untuk staf berhasil diambil", HttpStatus.OK)
                .toBuilder().build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /payments/reservation/{reservationId} — Get payment by reservation
    // ──────────────────────────────────────────────────────────────────────────

    @GetMapping("/reservation/{reservationId}")
    @PreAuthorize("hasAnyAuthority('STAF_LAPANGAN', 'STAF_TOKO', 'OWNER', 'ADMIN')")
    public ResponseEntity<BaseResponseDto<PembayaranResponse>> getPaymentByReservation(
            @PathVariable UUID reservationId) {
        PembayaranResponse payment = pembayaranService.getPaymentByReservationId(reservationId);
        return ResponseUtil.success(payment, "Data pembayaran berhasil diambil", HttpStatus.OK)
                .toBuilder().build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /payments/{reservasiId}/upload-proof — Upload payment proof (Customer)
    // ──────────────────────────────────────────────────────────────────────────

    @PostMapping(value = "/{reservasiId}/upload-proof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponseDto<ReservasiResponse>> uploadPaymentProof(
            @PathVariable UUID reservasiId,
            @RequestParam("file") MultipartFile file) {
        ReservasiResponse response = pembayaranService.uploadPaymentProof(reservasiId, file);
        return ResponseUtil.success(response,
                "Bukti pembayaran berhasil diunggah", HttpStatus.OK)
                .toBuilder().build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /payments/proof/{filename} — Serve uploaded payment proof image
    // ──────────────────────────────────────────────────────────────────────────

    @GetMapping("/proof/{filename}")
    public ResponseEntity<Resource> getPaymentProof(@PathVariable String filename) throws Exception {
        Path filePath = Paths.get(uploadDir).resolve(filename).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = "image/jpeg";
        String lowerFilename = filename.toLowerCase();
        if (lowerFilename.endsWith(".png"))
            contentType = "image/png";
        else if (lowerFilename.endsWith(".webp"))
            contentType = "image/webp";
        else if (lowerFilename.endsWith(".heic"))
            contentType = "image/heic";
        else if (lowerFilename.endsWith(".heif"))
            contentType = "image/heif";
        else if (lowerFilename.endsWith(".bmp"))
            contentType = "image/bmp";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PUT /payments/confirm/{reservasiId} — Confirm payment (Staff)
    // ──────────────────────────────────────────────────────────────────────────

    @PutMapping("/confirm/{reservasiId}")
    @PreAuthorize("hasAnyAuthority('STAF_LAPANGAN', 'STAF_TOKO', 'OWNER', 'ADMIN')")
    public ResponseEntity<BaseResponseDto<ConfirmPaymentResponse>> confirmPayment(
            @PathVariable UUID reservasiId) {
        ConfirmPaymentResponse response = pembayaranService.confirmPayment(reservasiId);
        return ResponseUtil.success(response, "Pembayaran berhasil dikonfirmasi", HttpStatus.OK)
                .toBuilder().build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PUT /payments/reject/{reservasiId} — Reject payment (Staff)
    // ──────────────────────────────────────────────────────────────────────────

    @PutMapping("/reject/{reservasiId}")
    @PreAuthorize("hasAnyAuthority('STAF_LAPANGAN', 'STAF_TOKO', 'OWNER', 'ADMIN')")
    public ResponseEntity<BaseResponseDto<ConfirmPaymentResponse>> rejectPayment(
            @PathVariable UUID reservasiId) {
        ConfirmPaymentResponse response = pembayaranService.rejectPayment(reservasiId);
        return ResponseUtil.success(response, "Reservasi berhasil ditolak", HttpStatus.OK)
                .toBuilder().build();
    }
}
