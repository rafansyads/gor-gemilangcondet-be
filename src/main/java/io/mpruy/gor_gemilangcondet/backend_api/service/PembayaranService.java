package io.mpruy.gor_gemilangcondet.backend_api.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.ConfirmPaymentResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.InvoiceResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.InvoiceSlotItem;
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
import io.mpruy.gor_gemilangcondet.backend_api.exception.BadRequestException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ConflictException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ResourceNotFoundException;
import io.mpruy.gor_gemilangcondet.backend_api.repository.LapanganRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.PembayaranRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.ReservasiRepository;
import io.mpruy.gor_gemilangcondet.backend_api.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PembayaranService {

    private final PembayaranRepository pembayaranRepository;
    private final ReservasiRepository reservasiRepository;
    private final LapanganRepository lapanganRepository;

    private static final ZoneId ZONE_JAKARTA = ZoneId.of("Asia/Jakarta");

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".png", ".jpg", ".jpeg", ".heic", ".heif", ".webp", ".bmp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png", "image/jpeg", "image/heic", "image/heif",
            "image/heif-sequence", "image/heic-sequence", "image/webp", "image/bmp", "image/x-bmp");

    @Value("${app.upload.dir:uploads/payment-proofs}")
    private String uploadDir;

    // ──────────────────────────────────────────────────────────────────────────
    // Get Unpaid Reservations (for staff dashboard) — legacy
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ReservasiResponse> getUnpaidReservations() {
        return reservasiRepository
                .findByStatusWithLapangan(ReservasiStatus.BELUM_DIBAYAR).stream()
                .map(this::toReservasiResponse)
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Get Reservations Pending Staff Confirmation
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns reservations waiting for staff confirmation (payment proof uploaded).
     * Includes both MENUNGGU_KONFIRMASI_STAF and DIKONFIRMASI for the staff list
     * view.
     */
    @Transactional(readOnly = true)
    public List<ReservasiResponse> getReservationsForStaff() {
        List<ReservasiStatus> staffStatuses = List.of(
                ReservasiStatus.MENUNGGU_KONFIRMASI_STAF,
                ReservasiStatus.DIKONFIRMASI);
        return reservasiRepository
                .findByStatusInWithLapangan(staffStatuses).stream()
                .map(this::toReservasiResponse)
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Get Staff Invoices (Invoice-centric view, grouped by batchId)
    // ──────────────────────────────────────────────────────────────────────────

    private static final DateTimeFormatter INV_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoicesForStaff() {
        List<ReservasiStatus> staffStatuses = List.of(
                ReservasiStatus.MENUNGGU_KONFIRMASI_STAF,
                ReservasiStatus.DIKONFIRMASI);

        List<Reservasi> reservations = reservasiRepository.findByStatusInWithLapangan(staffStatuses);

        // Group by batchId; singletons use their own ID as key
        Map<String, List<Reservasi>> grouped = new LinkedHashMap<>();
        for (Reservasi r : reservations) {
            String key = r.getBatchId() != null ? r.getBatchId().toString() : r.getId().toString();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }

        return grouped.entrySet().stream().map(entry -> {
            List<Reservasi> group = entry.getValue();
            group.sort(Comparator.comparing(Reservasi::getReservationStart));
            Reservasi first = group.get(0);

            double total = group.stream().mapToDouble(Reservasi::getTotalPayment).sum();

            String dateStr = first.getCreatedAt().format(INV_DATE_FMT);
            String shortId = first.getId().toString().substring(0, 5).toUpperCase();
            String invoiceNumber = "INV-" + dateStr + "-" + shortId;

            List<InvoiceSlotItem> slots = group.stream().map(r -> InvoiceSlotItem.builder()
                    .reservationId(r.getId().toString())
                    .courtName(r.getLapangan().getName())
                    .start(r.getReservationStart())
                    .end(r.getReservationEnd())
                    .price(r.getTotalPayment())
                    .build()).collect(Collectors.toList());

            return InvoiceResponse.builder()
                    .invoiceId(entry.getKey())
                    .invoiceNumber(invoiceNumber)
                    .representativeName(first.getNamaWakil())
                    .totalAmount(total)
                    .slotCount(group.size())
                    .status(first.getStatus().name())
                    .primaryReservationId(first.getId().toString())
                    .paymentProofUrl(first.getPaymentProofUrl())
                    .createdAt(first.getCreatedAt())
                    .slots(slots)
                    .build();
        })
                .sorted(Comparator.comparing(InvoiceResponse::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Get Payment by Reservation ID
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PembayaranResponse getPaymentByReservationId(UUID reservationId) {
        Pembayaran pembayaran = pembayaranRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pembayaran tidak ditemukan untuk reservasi: " + reservationId));
        return toPembayaranResponse(pembayaran);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Upload Payment Proof (Customer Action)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Customer uploads a payment proof image for their reservation.
     * <ol>
     * <li>Validates reservation exists and is BELUM_DIBAYAR</li>
     * <li>Validates payment deadline has not passed</li>
     * <li>Stores the uploaded file</li>
     * <li>Updates reservation: paymentProofUrl, status →
     * MENUNGGU_KONFIRMASI_STAF</li>
     * </ol>
     */
    @Transactional
    public ReservasiResponse uploadPaymentProof(UUID reservasiId, MultipartFile file) {
        LocalDateTime now = LocalDateTime.now(ZONE_JAKARTA);

        // 1. Find and validate reservation
        Reservasi reservasi = reservasiRepository.findByIdWithLapangan(reservasiId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reservasi tidak ditemukan: " + reservasiId));

        if (reservasi.getStatus() != ReservasiStatus.BELUM_DIBAYAR) {
            throw new ConflictException(
                    "Bukti pembayaran hanya bisa diunggah untuk reservasi dengan status BELUM_DIBAYAR. " +
                            "Status saat ini: " + reservasi.getStatus());
        }

        // 2. Check deadline
        if (reservasi.getPaymentDeadline() != null && now.isAfter(reservasi.getPaymentDeadline())) {
            // Auto-expire
            reservasi.setStatus(ReservasiStatus.EXPIRED);
            reservasi.setUpdatedAt(now);
            reservasiRepository.save(reservasi);
            throw new ConflictException("Batas waktu pembayaran telah habis. Reservasi otomatis kedaluwarsa.");
        }

        // 3. Validate file
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File bukti pembayaran wajib diunggah");
        }

        // 4. Validate file type (images only)
        validatePaymentProofFileType(file);

        // 5. Store file
        String filename = savePaymentProofFile(reservasiId, file);

        // 6. Update reservation
        reservasi.setPaymentProofUrl(filename);
        reservasi.setStatus(ReservasiStatus.MENUNGGU_KONFIRMASI_STAF);
        reservasi.setUpdatedAt(now);
        reservasiRepository.save(reservasi);

        // 7. If this reservation is part of a batch, propagate proof + status to
        // siblings
        if (reservasi.getBatchId() != null) {
            reservasiRepository.findByBatchId(reservasi.getBatchId()).stream()
                    .filter(r -> !r.getId().equals(reservasi.getId()))
                    .forEach(sibling -> {
                        sibling.setPaymentProofUrl(filename);
                        sibling.setStatus(ReservasiStatus.MENUNGGU_KONFIRMASI_STAF);
                        sibling.setUpdatedAt(now);
                        reservasiRepository.save(sibling);
                    });
        }

        log.info("Payment proof uploaded for reservation {}: {}", reservasiId, filename);

        return toReservasiResponse(reservasi);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Confirm Payment (Staff Action)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Staff confirms a reservation payment after reviewing the proof.
     * <ol>
     * <li>Validates reservation exists and is MENUNGGU_KONFIRMASI_STAF</li>
     * <li>Creates Pembayaran record with status LUNAS</li>
     * <li>Updates Reservasi status to DIKONFIRMASI</li>
     * <li>Marks Lapangan as DISEWAKAN</li>
     * </ol>
     */
    @Transactional
    public ConfirmPaymentResponse confirmPayment(UUID reservasiId) {
        LocalDateTime now = LocalDateTime.now(ZONE_JAKARTA);

        // 1. Find and validate reservation
        Reservasi reservasi = reservasiRepository.findByIdWithLapangan(reservasiId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reservasi tidak ditemukan: " + reservasiId));

        if (reservasi.getStatus() == ReservasiStatus.DIKONFIRMASI) {
            throw new ConflictException("Reservasi sudah dikonfirmasi");
        }
        if (reservasi.getStatus() == ReservasiStatus.DITOLAK) {
            throw new ConflictException("Reservasi sudah ditolak, tidak dapat dikonfirmasi");
        }
        if (reservasi.getStatus() == ReservasiStatus.DIBATALKAN) {
            throw new ConflictException("Reservasi sudah dibatalkan, tidak dapat dikonfirmasi");
        }
        if (reservasi.getStatus() == ReservasiStatus.EXPIRED) {
            throw new ConflictException("Reservasi sudah kedaluwarsa, tidak dapat dikonfirmasi");
        }
        if (reservasi.getStatus() != ReservasiStatus.MENUNGGU_KONFIRMASI_STAF) {
            throw new ConflictException(
                    "Hanya reservasi dengan status MENUNGGU_KONFIRMASI_STAF yang dapat dikonfirmasi. " +
                            "Status saat ini: " + reservasi.getStatus());
        }

        // 2. Get staff user
        UUID staffId = getCurrentStaffId();

        // 3. Create Pembayaran record
        Pembayaran pembayaran = Pembayaran.builder()
                .userId(reservasi.getUserId())
                .staffId(staffId)
                .reservationId(reservasi.getId())
                .method(PaymentMethod.TRANSFER)
                .type(PaymentType.FULL_PAYMENT_RESERVASI)
                .status(PaymentStatus.LUNAS)
                .price(reservasi.getTotalPayment())
                .receipt(reservasi.getPaymentProofUrl())
                .paymentDate(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        pembayaranRepository.save(pembayaran);

        // 4. Update Reservasi status to DIKONFIRMASI
        reservasi.setPaymentId(pembayaran.getId());
        reservasi.setStatus(ReservasiStatus.DIKONFIRMASI);
        reservasi.setUpdatedAt(now);
        reservasiRepository.save(reservasi);

        // 5. If batch: confirm all siblings too
        if (reservasi.getBatchId() != null) {
            UUID batchId = reservasi.getBatchId();
            reservasiRepository.findByBatchId(batchId).stream()
                    .filter(r -> !r.getId().equals(reservasi.getId()))
                    .forEach(sibling -> {
                        sibling.setPaymentId(pembayaran.getId());
                        sibling.setStatus(ReservasiStatus.DIKONFIRMASI);
                        sibling.setUpdatedAt(now);
                        reservasiRepository.save(sibling);
                    });
        }

        log.info("Staff {} confirmed reservation {}", staffId, reservasiId);

        // 6. Build response
        return ConfirmPaymentResponse.builder()
                .reservationId(reservasi.getId())
                .reservationStatus(ReservasiStatus.DIKONFIRMASI)
                .paymentId(pembayaran.getId())
                .paymentStatus(PaymentStatus.LUNAS)
                .lapanganName(reservasi.getLapangan().getName())
                .message("Pembayaran berhasil dikonfirmasi")
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Reject Payment (Staff Action)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Staff rejects a reservation after reviewing payment proof.
     * Reservation status → DITOLAK, schedule freed.
     */
    @Transactional
    public ConfirmPaymentResponse rejectPayment(UUID reservasiId) {
        LocalDateTime now = LocalDateTime.now(ZONE_JAKARTA);

        Reservasi reservasi = reservasiRepository.findByIdWithLapangan(reservasiId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reservasi tidak ditemukan: " + reservasiId));

        if (reservasi.getStatus() != ReservasiStatus.MENUNGGU_KONFIRMASI_STAF) {
            throw new ConflictException(
                    "Hanya reservasi dengan status MENUNGGU_KONFIRMASI_STAF yang dapat ditolak. " +
                            "Status saat ini: " + reservasi.getStatus());
        }

        UUID staffId = getCurrentStaffId();

        // Update reservation
        reservasi.setStatus(ReservasiStatus.DITOLAK);
        reservasi.setUpdatedAt(now);
        reservasiRepository.save(reservasi);

        // Reset Lapangan to TERSEDIA
        Lapangan lapangan = reservasi.getLapangan();
        if (lapangan != null && lapangan.getStatus() != LapanganStatus.TERSEDIA) {
            lapangan.setStatus(LapanganStatus.TERSEDIA);
            lapangan.setUpdatedAt(now);
            lapanganRepository.save(lapangan);
        }

        log.info("Staff {} rejected reservation {}", staffId, reservasiId);

        return ConfirmPaymentResponse.builder()
                .reservationId(reservasi.getId())
                .reservationStatus(ReservasiStatus.DITOLAK)
                .paymentId(null)
                .paymentStatus(null)
                .lapanganName(reservasi.getLapangan().getName())
                .message("Reservasi ditolak")
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Expire Overdue Reservations (called by scheduler)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Expires all reservations past their payment deadline that are still
     * BELUM_DIBAYAR.
     * Called periodically by PaymentScheduler.
     */
    @Transactional
    public int expireOverdueReservations() {
        LocalDateTime now = LocalDateTime.now(ZONE_JAKARTA);
        List<Reservasi> expired = reservasiRepository.findExpiredReservations(now);

        for (Reservasi reservasi : expired) {
            reservasi.setStatus(ReservasiStatus.EXPIRED);
            reservasi.setUpdatedAt(now);
            reservasiRepository.save(reservasi);

            Lapangan lapangan = reservasi.getLapangan();
            if (lapangan != null && lapangan.getStatus() != LapanganStatus.TERSEDIA) {
                lapangan.setStatus(LapanganStatus.TERSEDIA);
                lapangan.setUpdatedAt(now);
                lapanganRepository.save(lapangan);
            }

            log.info("Reservation {} expired (deadline: {})", reservasi.getId(), reservasi.getPaymentDeadline());
        }

        return expired.size();
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

    /**
     * Validates that the uploaded file is an allowed image format.
     * Allowed: PNG, JPG/JPEG, HEIC, HEIF, WEBP, BMP.
     */
    private void validatePaymentProofFileType(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException(
                    "Format file tidak didukung. Format yang diperbolehkan: PNG, JPG, JPEG, HEIC, HEIF, WEBP, BMP");
        }
        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException(
                    "Tipe konten file tidak didukung. Hanya file gambar yang diperbolehkan.");
        }
    }

    /**
     * Saves the uploaded payment proof file and returns the stored filename.
     */
    private String savePaymentProofFile(UUID reservasiId, MultipartFile file) {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = "proof_" + reservasiId + "_" + System.currentTimeMillis() + extension;

            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return filename;
        } catch (IOException e) {
            throw new RuntimeException("Gagal menyimpan file bukti pembayaran: " + e.getMessage(), e);
        }
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
        int duration = (int) ChronoUnit.HOURS.between(
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
                .paymentProofUrl(reservasi.getPaymentProofUrl())
                .paymentDeadline(reservasi.getPaymentDeadline())
                .createdAt(reservasi.getCreatedAt())
                .updatedAt(reservasi.getUpdatedAt())
                .build();
    }
}
