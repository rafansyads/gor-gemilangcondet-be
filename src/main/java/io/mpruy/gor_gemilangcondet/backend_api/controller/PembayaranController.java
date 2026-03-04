package io.mpruy.gor_gemilangcondet.backend_api.controller;

import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseResponseDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.ConfirmPaymentResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.PembayaranResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.ReservasiResponse;
import io.mpruy.gor_gemilangcondet.backend_api.service.PembayaranService;
import io.mpruy.gor_gemilangcondet.backend_api.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PembayaranController {

    private final PembayaranService pembayaranService;

    // ──────────────────────────────────────────────────────────────────────────
    // GET /payments/unpaid — List all unpaid reservations (Staff)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns all reservations with BELUM_DIBAYAR status.
     * Restricted to staff and admin roles.
     */
    @GetMapping("/unpaid")
    @PreAuthorize("hasAnyAuthority('STAF_LAPANGAN', 'STAF_TOKO', 'OWNER', 'ADMIN')")
    public ResponseEntity<BaseResponseDto<List<ReservasiResponse>>> getUnpaidReservations() {
        try {
            List<ReservasiResponse> unpaid = pembayaranService.getUnpaidReservations();
            return ResponseUtil.success(unpaid,
                    "Data reservasi belum dibayar berhasil diambil", HttpStatus.OK)
                    .toBuilder().build();
        } catch (Exception ex) {
            return ResponseUtil.error(
                    "Gagal mengambil data reservasi belum dibayar: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /payments/reservation/{reservationId} — Get payment by reservation
    // ──────────────────────────────────────────────────────────────────────────

    @GetMapping("/reservation/{reservationId}")
    @PreAuthorize("hasAnyAuthority('STAF_LAPANGAN', 'STAF_TOKO', 'OWNER', 'ADMIN')")
    public ResponseEntity<BaseResponseDto<PembayaranResponse>> getPaymentByReservation(
            @PathVariable UUID reservationId) {
        try {
            PembayaranResponse payment = pembayaranService.getPaymentByReservationId(reservationId);
            return ResponseUtil.success(payment, "Data pembayaran berhasil diambil", HttpStatus.OK)
                    .toBuilder().build();
        } catch (IllegalArgumentException ex) {
            return ResponseUtil.error(ex.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            return ResponseUtil.error(
                    "Gagal mengambil data pembayaran: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PUT /payments/confirm/{reservasiId} — Confirm payment (Staff)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Staff clicks "Sudah Membayar" on a reservation detail page.
     * No request body needed — just the reservation ID in the URL.
     * Changes reservation status: BELUM_DIBAYAR → DIBAYAR
     * and marks the court as DISEWAKAN.
     */
    @PutMapping("/confirm/{reservasiId}")
    @PreAuthorize("hasAnyAuthority('STAF_LAPANGAN', 'STAF_TOKO', 'OWNER', 'ADMIN')")
    public ResponseEntity<BaseResponseDto<ConfirmPaymentResponse>> confirmPayment(
            @PathVariable UUID reservasiId) {
        try {
            ConfirmPaymentResponse response = pembayaranService.confirmPayment(reservasiId);
            return ResponseUtil.success(response, "Pembayaran berhasil dikonfirmasi", HttpStatus.OK)
                    .toBuilder().build();
        } catch (IllegalArgumentException ex) {
            return ResponseUtil.error(ex.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (IllegalStateException ex) {
            return ResponseUtil.error(ex.getMessage(), HttpStatus.CONFLICT);
        } catch (Exception ex) {
            return ResponseUtil.error(
                    "Gagal mengkonfirmasi pembayaran: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
