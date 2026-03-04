package io.mpruy.gor_gemilangcondet.backend_api.service;

import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.ConfirmPaymentResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.PembayaranResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.ReservasiResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.payment.PaymentMethod;
import io.mpruy.gor_gemilangcondet.backend_api.entities.payment.PaymentStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.payment.PaymentType;
import io.mpruy.gor_gemilangcondet.backend_api.entities.payment.Pembayaran;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.Lapangan;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.LapanganStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.Reservasi;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.ReservasiStatus;
import io.mpruy.gor_gemilangcondet.backend_api.repository.LapanganRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.PembayaranRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.ReservasiRepository;
import io.mpruy.gor_gemilangcondet.backend_api.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PembayaranService {

    private final PembayaranRepository pembayaranRepository;
    private final ReservasiRepository reservasiRepository;
    private final LapanganRepository lapanganRepository;

    private static final ZoneId ZONE_JAKARTA = ZoneId.of("Asia/Jakarta");

    // ──────────────────────────────────────────────────────────────────────────
    // Get Unpaid Reservations (for staff dashboard)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns all reservations with status BELUM_DIBAYAR.
     * Intended for staff to see which reservations are awaiting payment
     * confirmation.
     */
    @Transactional(readOnly = true)
    public List<ReservasiResponse> getUnpaidReservations() {
        return reservasiRepository
                .findByStatusWithLapangan(ReservasiStatus.BELUM_DIBAYAR).stream()
                .map(this::toReservasiResponse)
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Get Payment by Reservation ID
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PembayaranResponse getPaymentByReservationId(UUID reservationId) {
        Pembayaran pembayaran = pembayaranRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pembayaran tidak ditemukan untuk reservasi: " + reservationId));
        return toPembayaranResponse(pembayaran);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Confirm Payment (Staff Action — no body, just reservasiId)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Staff confirms that a reservation has been paid.
     * This is an atomic operation:
     * <ol>
     * <li>Validates reservation exists and is still BELUM_DIBAYAR</li>
     * <li>Creates the Pembayaran record with status LUNAS</li>
     * <li>Updates Reservasi status to DIBAYAR</li>
     * <li>Marks the Lapangan as DISEWAKAN for the reserved time slot</li>
     * </ol>
     *
     * All steps succeed or fail together within the same transaction.
     *
     * @param reservasiId the ID of the reservation to confirm payment for
     * @return confirmation response with updated statuses
     */
    @Transactional
    public ConfirmPaymentResponse confirmPayment(UUID reservasiId) {
        LocalDateTime now = LocalDateTime.now(ZONE_JAKARTA);

        // 1. Find and validate reservation
        Reservasi reservasi = reservasiRepository.findByIdWithLapangan(reservasiId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Reservasi tidak ditemukan: " + reservasiId));

        if (reservasi.getStatus() == ReservasiStatus.DIBAYAR) {
            throw new IllegalStateException("Reservasi sudah dibayar, tidak dapat dikonfirmasi ulang");
        }
        if (reservasi.getStatus() == ReservasiStatus.DIBATALKAN) {
            throw new IllegalStateException("Reservasi sudah dibatalkan, tidak dapat dikonfirmasi");
        }
        if (reservasi.getStatus() == ReservasiStatus.SELESAI) {
            throw new IllegalStateException("Reservasi sudah selesai, tidak dapat dikonfirmasi");
        }
        if (reservasi.getStatus() != ReservasiStatus.BELUM_DIBAYAR) {
            throw new IllegalStateException(
                    "Status reservasi tidak valid untuk konfirmasi pembayaran: " + reservasi.getStatus());
        }

        // 2. Get the staff user from SecurityContext
        UUID staffId = getCurrentStaffId();

        // 3. Create Pembayaran record (full payment, CASH as default)
        Pembayaran pembayaran = Pembayaran.builder()
                .userId(reservasi.getUserId())
                .staffId(staffId)
                .reservationId(reservasi.getId())
                .method(PaymentMethod.CASH)
                .type(PaymentType.FULL_PAYMENT_RESERVASI)
                .status(PaymentStatus.LUNAS)
                .price(reservasi.getTotalPayment())
                .paymentDate(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        pembayaranRepository.save(pembayaran);

        // 4. Update Reservasi status to DIBAYAR
        reservasi.setPaymentId(pembayaran.getId());
        reservasi.setStatus(ReservasiStatus.DIBAYAR);
        reservasi.setUpdatedAt(now);
        reservasiRepository.save(reservasi);

        // 5. Mark Lapangan as DISEWAKAN (booked)
        Lapangan lapangan = reservasi.getLapangan();
        lapangan.setStatus(LapanganStatus.DISEWAKAN);
        lapangan.setUpdatedAt(now);
        lapanganRepository.save(lapangan);

        // 6. Build response
        return ConfirmPaymentResponse.builder()
                .reservationId(reservasi.getId())
                .reservationStatus(ReservasiStatus.DIBAYAR)
                .paymentId(pembayaran.getId())
                .paymentStatus(PaymentStatus.LUNAS)
                .lapanganName(lapangan.getName())
                .message("Pembayaran berhasil dikonfirmasi")
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private UUID getCurrentStaffId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            return userDetails.getUser().getId();
        }
        throw new IllegalStateException("Staff tidak terautentikasi");
    }

    private PembayaranResponse toPembayaranResponse(Pembayaran pembayaran) {
        return PembayaranResponse.builder()
                .id(pembayaran.getId())
                .userId(pembayaran.getUserId())
                .staffId(pembayaran.getStaffId())
                .reservationId(pembayaran.getReservationId())
                .method(pembayaran.getMethod())
                .type(pembayaran.getType())
                .status(pembayaran.getStatus())
                .price(pembayaran.getPrice())
                .receipt(pembayaran.getReceipt())
                .paymentDate(pembayaran.getPaymentDate())
                .createdAt(pembayaran.getCreatedAt())
                .updatedAt(pembayaran.getUpdatedAt())
                .build();
    }

    private ReservasiResponse toReservasiResponse(Reservasi reservasi) {
        int duration = (int) java.time.temporal.ChronoUnit.HOURS.between(
                reservasi.getReservationStart(), reservasi.getReservationEnd());

        return ReservasiResponse.builder()
                .id(reservasi.getId())
                .userId(reservasi.getUserId())
                .lapanganId(reservasi.getLapangan().getId())
                .lapanganName(reservasi.getLapangan().getName())
                .lapanganType(reservasi.getLapangan().getType())
                .reservationStart(reservasi.getReservationStart())
                .reservationEnd(reservasi.getReservationEnd())
                .durationInHours(duration)
                .namaWakil(reservasi.getNamaWakil())
                .nomorTelepon(reservasi.getNomorTelepon())
                .jumlahOrang(reservasi.getJumlahOrang())
                .totalPayment(reservasi.getTotalPayment())
                .status(reservasi.getStatus())
                .createdAt(reservasi.getCreatedAt())
                .updatedAt(reservasi.getUpdatedAt())
                .build();
    }
}
