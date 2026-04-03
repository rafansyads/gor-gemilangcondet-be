package io.mpruy.gor_gemilangcondet.backend_api.service;

import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.requests.*;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.*;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.*;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.AlatOlahraga;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.AlatOlahragaStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.BarangType;
import io.mpruy.gor_gemilangcondet.backend_api.exception.BadRequestException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ResourceNotFoundException;
import io.mpruy.gor_gemilangcondet.backend_api.repository.AlatOlahragaRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.LapanganRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.ReservasiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservasiService {

        private final LapanganRepository lapanganRepository;
        private final ReservasiRepository reservasiRepository;
        private final AlatOlahragaRepository alatOlahragaRepository;

        private static final ZoneId ZONE_JAKARTA = ZoneId.of("Asia/Jakarta");
        private static final int OPENING_HOUR = 6;
        private static final int CLOSING_HOUR = 23;

        /** Payment deadline in minutes from reservation creation. */
        private static final int PAYMENT_DEADLINE_MINUTES = 10;

        /** Reservation statuses considered inactive (slots freed). */
        private static final List<ReservasiStatus> INACTIVE_STATUSES = List.of(
                        ReservasiStatus.DIBATALKAN,
                        ReservasiStatus.DITOLAK,
                        ReservasiStatus.EXPIRED,
                        ReservasiStatus.SELESAI);

        /** Mapping from court type to compatible equipment types for rental. */
        private static final Map<LapanganType, List<BarangType>> COURT_EQUIPMENT_MAP = Map.of(
                        LapanganType.BADMINTON, List.of(BarangType.RAKET, BarangType.AKSESORIS, BarangType.SEPATU),
                        LapanganType.FUTSAL, List.of(BarangType.BOLA, BarangType.SEPATU),
                        LapanganType.BASKET, List.of(BarangType.BOLA, BarangType.SEPATU),
                        LapanganType.VOLI, List.of(BarangType.BOLA, BarangType.SEPATU),
                        LapanganType.TENIS, List.of(BarangType.RAKET, BarangType.BOLA, BarangType.SEPATU));

        // ──────────────────────────────────────────────────────────────────────────
        // Get All Courts
        // ──────────────────────────────────────────────────────────────────────────

        @Transactional(readOnly = true)
        public List<LapanganResponse> getAllCourts() {
                return lapanganRepository.findAll().stream()
                                .sorted(Comparator.comparing(Lapangan::getName))
                                .map(this::toLapanganResponse)
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public List<LapanganResponse> getCourtsByType(LapanganType type) {
                return lapanganRepository.findByType(type).stream()
                                .sorted(Comparator.comparing(Lapangan::getName))
                                .map(this::toLapanganResponse)
                                .collect(Collectors.toList());
        }

        // ──────────────────────────────────────────────────────────────────────────
        // Court Availability
        // ──────────────────────────────────────────────────────────────────────────

        /**
         * Returns per-court, per-hour-slot availability for the given date.
         * If the date is today, only slots starting from the next full hour are shown
         * (e.g. at 11:59 the first visible slot is 12:00-13:00).
         */
        @Transactional(readOnly = true)
        public List<CourtAvailabilityResponse> getAvailability(LocalDate date, LapanganType type) {
                List<Lapangan> courts = ((type != null)
                                ? lapanganRepository.findByType(type)
                                : lapanganRepository.findAll())
                                .stream()
                                .sorted(Comparator.comparing(Lapangan::getName))
                                .collect(Collectors.toList());

                LocalDateTime dayStart = date.atTime(OPENING_HOUR, 0);
                LocalDateTime dayEnd = date.atTime(CLOSING_HOUR, 0);

                // Determine the earliest visible slot (next full hour if today)
                LocalDateTime now = LocalDateTime.now(ZONE_JAKARTA);
                int firstSlotHour = OPENING_HOUR;

                if (date.equals(now.toLocalDate())) {
                        firstSlotHour = now.getHour() + 1;
                        if (firstSlotHour < OPENING_HOUR)
                                firstSlotHour = OPENING_HOUR;
                        if (firstSlotHour >= CLOSING_HOUR) {
                                // No more slots available today
                                return courts.stream()
                                                .map(c -> CourtAvailabilityResponse.builder()
                                                                .lapanganId(c.getId())
                                                                .lapanganName(c.getName())
                                                                .lapanganType(c.getType())
                                                                .tarifPerJam(c.getTarifPerJam())
                                                                .date(date)
                                                                .slots(Collections.emptyList())
                                                                .build())
                                                .collect(Collectors.toList());
                        }
                }

                List<CourtAvailabilityResponse> result = new ArrayList<>();

                for (Lapangan court : courts) {
                        // Courts under maintenance are fully unavailable
                        if (court.getStatus() == LapanganStatus.DALAM_PERBAIKAN) {
                                List<SlotAvailabilityResponse> allUnavailable = new ArrayList<>();
                                for (int h = firstSlotHour; h < CLOSING_HOUR; h++) {
                                        allUnavailable.add(SlotAvailabilityResponse.builder()
                                                        .startHour(h).endHour(h + 1).available(false).build());
                                }
                                result.add(CourtAvailabilityResponse.builder()
                                                .lapanganId(court.getId())
                                                .lapanganName(court.getName())
                                                .lapanganType(court.getType())
                                                .tarifPerJam(court.getTarifPerJam())
                                                .date(date)
                                                .slots(allUnavailable)
                                                .build());
                                continue;
                        }

                        // Find active reservations for this court on this date
                        List<Reservasi> reservations = reservasiRepository.findOverlappingReservations(
                                        court.getId(), dayStart, dayEnd, INACTIVE_STATUSES);

                        List<SlotAvailabilityResponse> slots = new ArrayList<>();
                        for (int h = firstSlotHour; h < CLOSING_HOUR; h++) {
                                LocalDateTime slotStart = date.atTime(h, 0);
                                LocalDateTime slotEnd = date.atTime(h + 1, 0);

                                boolean isAvailable = reservations.stream()
                                                .noneMatch(r -> r.getReservationStart().isBefore(slotEnd) &&
                                                                r.getReservationEnd().isAfter(slotStart));

                                // Check maintenance window
                                if (court.getMaintenanceStart() != null && court.getMaintenanceEnd() != null) {
                                        if (court.getMaintenanceStart().isBefore(slotEnd) &&
                                                        court.getMaintenanceEnd().isAfter(slotStart)) {
                                                isAvailable = false;
                                        }
                                }

                                slots.add(SlotAvailabilityResponse.builder()
                                                .startHour(h)
                                                .endHour(h + 1)
                                                .available(isAvailable)
                                                .build());
                        }

                        result.add(CourtAvailabilityResponse.builder()
                                        .lapanganId(court.getId())
                                        .lapanganName(court.getName())
                                        .lapanganType(court.getType())
                                        .tarifPerJam(court.getTarifPerJam())
                                        .date(date)
                                        .slots(slots)
                                        .build());
                }

                return result;
        }

        // ──────────────────────────────────────────────────────────────────────────
        // Equipment Availability
        // ──────────────────────────────────────────────────────────────────────────

        /**
         * Returns available sports equipment for the given court type and time range.
         * Equipment types are filtered based on the court type mapping.
         */
        @Transactional(readOnly = true)
        public List<AlatOlahragaAvailabilityResponse> getEquipmentAvailability(
                        LapanganType courtType, LocalDateTime start, LocalDateTime end) {

                List<BarangType> relevantTypes = COURT_EQUIPMENT_MAP.getOrDefault(
                                courtType, Collections.emptyList());

                if (relevantTypes.isEmpty()) {
                        return Collections.emptyList();
                }

                List<AlatOlahraga> equipment = alatOlahragaRepository
                                .findByTypeInAndStatus(relevantTypes, AlatOlahragaStatus.TERSEDIA);

                // Count rented equipment during the requested period
                List<Reservasi> activeReservations = reservasiRepository
                                .findActiveReservationsDuringPeriod(start, end, INACTIVE_STATUSES);

                Map<UUID, Long> rentedCounts = activeReservations.stream()
                                .filter(r -> r.getRentList() != null)
                                .flatMap(r -> r.getRentList().stream())
                                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

                return equipment.stream().map(e -> {
                        long rented = rentedCounts.getOrDefault(e.getId(), 0L);
                        long available = Math.max(0, e.getStock() - rented);
                        return AlatOlahragaAvailabilityResponse.builder()
                                        .id(e.getId())
                                        .name(e.getName())
                                        .type(e.getType())
                                        .price(e.getPrice())
                                        .totalStock(e.getStock())
                                        .rented(rented)
                                        .available(available)
                                        .build();
                }).collect(Collectors.toList());
        }

        // ──────────────────────────────────────────────────────────────────────────
        // Create Reservation (Atomic — race-condition safe)
        // ──────────────────────────────────────────────────────────────────────────

        /**
         * Creates a new court reservation with atomic slot checking.
         * Uses pessimistic locking on the court row to prevent two concurrent
         * users from booking the same time slot.
         *
         * <p>
         * Business rules enforced:
         * </p>
         * <ul>
         * <li>Reservation date must not be in the past</li>
         * <li>Start time must be on the hour</li>
         * <li>Must fall within operating hours (06:00–23:00)</li>
         * <li>Court must be available (not under maintenance)</li>
         * <li>No overlapping active reservations</li>
         * <li>Equipment stock must be sufficient</li>
         * <li>Cost is automatically calculated (court tariff × hours + equipment)</li>
         * </ul>
         */
        @Transactional
        public ReservasiResponse createReservation(CreateReservasiRequest request) {
                LocalDateTime now = LocalDateTime.now(ZONE_JAKARTA);

                // 1. Validate: reservation date must not be in the past
                if (request.getReservationStart().isBefore(now)) {
                        throw new BadRequestException("Tanggal reservasi tidak boleh di masa lalu");
                }

                // 2. Validate: reservation start must be on the hour
                if (request.getReservationStart().getMinute() != 0 ||
                                request.getReservationStart().getSecond() != 0) {
                        throw new BadRequestException(
                                        "Waktu reservasi harus dimulai pada jam tepat (contoh: 08:00, 09:00)");
                }

                // 3. Validate: within operating hours
                int startHour = request.getReservationStart().getHour();
                int endHour = startHour + request.getDurationInHours();
                if (startHour < OPENING_HOUR || endHour > CLOSING_HOUR) {
                        throw new BadRequestException(
                                        String.format("Jam operasional GOR: %02d:00 - %02d:00", OPENING_HOUR,
                                                        CLOSING_HOUR));
                }

                // 4. Lock the court (PESSIMISTIC_WRITE → SELECT ... FOR UPDATE)
                // This prevents race conditions when two users book simultaneously
                Lapangan lapangan = lapanganRepository.findByIdWithPessimisticLock(request.getLapanganId())
                                .orElseThrow(() -> new ResourceNotFoundException("Lapangan tidak ditemukan"));

                // 5. Check court status
                if (lapangan.getStatus() != LapanganStatus.TERSEDIA) {
                        throw new BadRequestException(
                                        "Lapangan sedang tidak tersedia (status: " + lapangan.getStatus() + ")");
                }

                // 6. Calculate reservation end time
                LocalDateTime reservationEnd = request.getReservationStart()
                                .plusHours(request.getDurationInHours());

                // 7. Check maintenance window
                if (lapangan.getMaintenanceStart() != null && lapangan.getMaintenanceEnd() != null) {
                        if (lapangan.getMaintenanceStart().isBefore(reservationEnd) &&
                                        lapangan.getMaintenanceEnd().isAfter(request.getReservationStart())) {
                                throw new BadRequestException(
                                                "Lapangan sedang dalam perbaikan pada waktu yang dipilih");
                        }
                }

                // 8. Atomic check: no overlapping active reservations
                List<Reservasi> overlapping = reservasiRepository.findOverlappingReservations(
                                lapangan.getId(), request.getReservationStart(), reservationEnd, INACTIVE_STATUSES);
                if (!overlapping.isEmpty()) {
                        throw new BadRequestException("Jadwal lapangan sudah terisi pada waktu yang dipilih");
                }

                // 9. Process equipment rental
                List<UUID> expandedRentList = new ArrayList<>();
                double equipmentCost = 0;
                if (request.getRentItems() != null && !request.getRentItems().isEmpty()) {
                        equipmentCost = processRentItems(
                                        request.getRentItems(),
                                        request.getReservationStart(),
                                        reservationEnd,
                                        expandedRentList);
                }

                // 10. Calculate total cost automatically
                double courtCost = lapangan.getTarifPerJam() * request.getDurationInHours();
                double totalCost = courtCost + equipmentCost;

                // 11. Build and save reservation
                Reservasi reservasi = Reservasi.builder()
                                .reservationStart(request.getReservationStart())
                                .reservationEnd(reservationEnd)
                                .lapangan(lapangan)
                                .userId(request.getUserId())
                                .namaWakil(request.getNamaWakil())
                                .nomorTelepon(request.getNomorTelepon())
                                .jumlahOrang(request.getJumlahOrang())
                                .totalPayment(totalCost)
                                .status(ReservasiStatus.BELUM_DIBAYAR)
                                .paymentDeadline(now.plusMinutes(PAYMENT_DEADLINE_MINUTES))
                                .rentList(expandedRentList)
                                .createdAt(now)
                                .updatedAt(now)
                                .build();

                reservasiRepository.save(reservasi);

                return toReservasiResponse(reservasi);
        }

        // ──────────────────────────────────────────────────────────────────────────
        // Reschedule Reservation
        // ──────────────────────────────────────────────────────────────────────────

        /**
         * Reschedules an existing reservation to a new time slot.
         * Applies the same validation and atomic locking as creation.
         * Optionally changes the court if newLapanganId is provided.
         */
        @Transactional
        public ReservasiResponse rescheduleReservation(UUID reservasiId, RescheduleReservasiRequest request) {
                LocalDateTime now = LocalDateTime.now(ZONE_JAKARTA);

                // 1. Find existing reservation
                Reservasi reservasi = reservasiRepository.findByIdWithLapangan(reservasiId)
                                .orElseThrow(() -> new ResourceNotFoundException("Reservasi tidak ditemukan"));

                // 2. Validate reservation can be rescheduled
                if (reservasi.getStatus() == ReservasiStatus.DIBATALKAN ||
                                reservasi.getStatus() == ReservasiStatus.SELESAI) {
                        throw new BadRequestException(
                                        "Reservasi yang sudah " + reservasi.getStatus()
                                                        + " tidak dapat dijadwal ulang");
                }

                // 3. Validate: new date must not be in the past
                if (request.getNewReservationStart().isBefore(now)) {
                        throw new BadRequestException("Tanggal reservasi baru tidak boleh di masa lalu");
                }

                // 4. Validate: must be on the hour
                if (request.getNewReservationStart().getMinute() != 0 ||
                                request.getNewReservationStart().getSecond() != 0) {
                        throw new BadRequestException("Waktu reservasi harus dimulai pada jam tepat");
                }

                // 5. Validate: within operating hours
                int startHour = request.getNewReservationStart().getHour();
                int endHour = startHour + request.getDurationInHours();
                if (startHour < OPENING_HOUR || endHour > CLOSING_HOUR) {
                        throw new BadRequestException(
                                        String.format("Jam operasional GOR: %02d:00 - %02d:00", OPENING_HOUR,
                                                        CLOSING_HOUR));
                }

                // 6. Determine target court (new or existing)
                UUID targetLapanganId = (request.getNewLapanganId() != null)
                                ? request.getNewLapanganId()
                                : reservasi.getLapangan().getId();

                // 7. Lock the target court
                Lapangan lapangan = lapanganRepository.findByIdWithPessimisticLock(targetLapanganId)
                                .orElseThrow(() -> new ResourceNotFoundException("Lapangan tidak ditemukan"));

                // 8. Validate court is available (not under maintenance)
                if (lapangan.getStatus() == LapanganStatus.DALAM_PERBAIKAN) {
                        throw new BadRequestException(
                                        "Lapangan sedang dalam perbaikan dan tidak dapat digunakan");
                }

                // 9. Validate the new court has the same type as the original
                if (lapangan.getType() != reservasi.getLapangan().getType()) {
                        throw new BadRequestException(
                                        "Lapangan baru harus memiliki tipe yang sama dengan reservasi awal (" +
                                        reservasi.getLapangan().getType() + ")");
                }

                // 10. Calculate new end time
                LocalDateTime newEnd = request.getNewReservationStart()
                                .plusHours(request.getDurationInHours());

                // 11. Check maintenance window on new court
                if (lapangan.getMaintenanceStart() != null && lapangan.getMaintenanceEnd() != null) {
                        if (lapangan.getMaintenanceStart().isBefore(newEnd) &&
                                        lapangan.getMaintenanceEnd().isAfter(request.getNewReservationStart())) {
                                throw new BadRequestException(
                                                "Lapangan sedang dalam perbaikan pada waktu yang dipilih");
                        }
                }

                // 12. Check for overlapping reservations (exclude current reservation)
                List<Reservasi> overlapping = reservasiRepository.findOverlappingReservations(
                                lapangan.getId(), request.getNewReservationStart(), newEnd, INACTIVE_STATUSES);
                overlapping.removeIf(r -> r.getId().equals(reservasiId));

                if (!overlapping.isEmpty()) {
                        throw new BadRequestException("Jadwal lapangan sudah terisi pada waktu baru yang dipilih");
                }

                // 13. Recalculate cost based on target court
                double courtCost = lapangan.getTarifPerJam() * request.getDurationInHours();
                double equipmentCost = calculateExistingEquipmentCost(reservasi.getRentList());
                double totalCost = courtCost + equipmentCost;

                // 14. Update reservation
                reservasi.setLapangan(lapangan);
                reservasi.setReservationStart(request.getNewReservationStart());
                reservasi.setReservationEnd(newEnd);
                reservasi.setTotalPayment(totalCost);
                reservasi.setUpdatedAt(now);

                reservasiRepository.save(reservasi);

                return toReservasiResponse(reservasi);
        }

        // ──────────────────────────────────────────────────────────────────────────
        // Court CRUD
        // ──────────────────────────────────────────────────────────────────────────

        @Transactional
        public LapanganResponse createCourt(CreateLapanganRequest request) {
                LocalDateTime now = LocalDateTime.now(ZONE_JAKARTA);
                Lapangan lapangan = Lapangan.builder()
                                .name(request.getName())
                                .type(request.getType())
                                .tarifPerJam(request.getTarifPerJam())
                                .status(LapanganStatus.TERSEDIA)
                                .createdAt(now)
                                .updatedAt(now)
                                .build();
                lapanganRepository.save(lapangan);
                return toLapanganResponse(lapangan);
        }

        @Transactional
        public LapanganResponse updateCourt(UUID id, UpdateLapanganRequest request) {
                Lapangan lapangan = lapanganRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Lapangan tidak ditemukan: " + id));
                lapangan.setName(request.getName());
                lapangan.setType(request.getType());
                lapangan.setTarifPerJam(request.getTarifPerJam());
                lapangan.setUpdatedAt(LocalDateTime.now(ZONE_JAKARTA));
                lapanganRepository.save(lapangan);
                return toLapanganResponse(lapangan);
        }

        @Transactional
        public void deleteCourt(UUID id) {
                Lapangan lapangan = lapanganRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Lapangan tidak ditemukan: " + id));
                // Check if court has active reservations
                LocalDateTime now = LocalDateTime.now(ZONE_JAKARTA);
                List<Reservasi> active = reservasiRepository.findOverlappingReservations(
                                id, now, now.plusYears(1), INACTIVE_STATUSES);
                if (!active.isEmpty()) {
                        throw new BadRequestException(
                                        "Tidak dapat menghapus lapangan yang masih memiliki reservasi aktif");
                }
                lapanganRepository.delete(lapangan);
        }

        @Transactional
        public LapanganResponse updateCourtStatus(UUID id, LapanganStatus newStatus) {
                Lapangan lapangan = lapanganRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Lapangan tidak ditemukan: " + id));
                lapangan.setStatus(newStatus);
                lapangan.setUpdatedAt(LocalDateTime.now(ZONE_JAKARTA));
                lapanganRepository.save(lapangan);
                return toLapanganResponse(lapangan);
        }

        // ──────────────────────────────────────────────────────────────────────────
        // Get Reservations
        // ──────────────────────────────────────────────────────────────────────────

        @Transactional(readOnly = true)
        public List<ReservasiResponse> getAllReservations() {
                return reservasiRepository.findAllWithLapangan().stream()
                                .map(this::toReservasiResponse)
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public ReservasiResponse getReservationById(UUID id) {
                Reservasi reservasi = reservasiRepository.findByIdWithLapangan(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Reservasi tidak ditemukan: " + id));
                return toReservasiResponse(reservasi);
        }

        @Transactional(readOnly = true)
        public List<ReservasiResponse> getReservationsByUserId(UUID userId) {
                return reservasiRepository.findByUserIdWithLapangan(userId).stream()
                                .map(this::toReservasiResponse)
                                .collect(Collectors.toList());
        }

        // ──────────────────────────────────────────────────────────────────────────
        // Private Helpers
        // ──────────────────────────────────────────────────────────────────────────

        /**
         * Validates and processes equipment rental items.
         * Checks availability against current stock minus already-rented quantities
         * during the overlapping period, then expands into a flat UUID list.
         *
         * @return total equipment rental cost
         */
        private double processRentItems(List<RentItemRequest> rentItems,
                        LocalDateTime start, LocalDateTime end,
                        List<UUID> expandedRentList) {
                // Count rented equipment during the requested period
                List<Reservasi> activeReservations = reservasiRepository
                                .findActiveReservationsDuringPeriod(start, end, INACTIVE_STATUSES);

                Map<UUID, Long> rentedCounts = activeReservations.stream()
                                .filter(r -> r.getRentList() != null)
                                .flatMap(r -> r.getRentList().stream())
                                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

                double totalEquipmentCost = 0;

                for (RentItemRequest item : rentItems) {
                        AlatOlahraga equipment = alatOlahragaRepository.findById(item.getAlatOlahragaId())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Alat olahraga tidak ditemukan: " + item.getAlatOlahragaId()));

                        if (equipment.getStatus() != AlatOlahragaStatus.TERSEDIA) {
                                throw new BadRequestException(
                                                "Alat olahraga '" + equipment.getName() + "' sedang tidak tersedia");
                        }

                        long currentlyRented = rentedCounts.getOrDefault(equipment.getId(), 0L);
                        long available = equipment.getStock() - currentlyRented;

                        if (item.getQuantity() > available) {
                                throw new BadRequestException(
                                                String.format("Stok '%s' tidak mencukupi. Tersedia: %d, Diminta: %d",
                                                                equipment.getName(), available, item.getQuantity()));
                        }

                        // Expand to flat list (each UUID entry = 1 unit rented)
                        for (int i = 0; i < item.getQuantity(); i++) {
                                expandedRentList.add(equipment.getId());
                        }

                        // Track cumulative rentals for subsequent items of the same type
                        rentedCounts.merge(equipment.getId(), (long) item.getQuantity(), Long::sum);

                        totalEquipmentCost += equipment.getPrice() * item.getQuantity();
                }

                return totalEquipmentCost;
        }

        /**
         * Calculates equipment cost for an existing rent list (used during reschedule).
         */
        private double calculateExistingEquipmentCost(List<UUID> rentList) {
                if (rentList == null || rentList.isEmpty())
                        return 0;

                Map<UUID, Long> counts = rentList.stream()
                                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

                double cost = 0;
                for (Map.Entry<UUID, Long> entry : counts.entrySet()) {
                        AlatOlahraga equipment = alatOlahragaRepository.findById(entry.getKey())
                                        .orElse(null);
                        if (equipment != null) {
                                cost += equipment.getPrice() * entry.getValue();
                        }
                }
                return cost;
        }

        // ──────────────────────────────────────────────────────────────────────────
        // Mappers
        // ──────────────────────────────────────────────────────────────────────────

        private LapanganResponse toLapanganResponse(Lapangan lapangan) {
                return LapanganResponse.builder()
                                .id(lapangan.getId())
                                .name(lapangan.getName())
                                .type(lapangan.getType())
                                .status(lapangan.getStatus())
                                .tarifPerJam(lapangan.getTarifPerJam())
                                .build();
        }

        private ReservasiResponse toReservasiResponse(Reservasi reservasi) {
                List<RentItemResponse> rentItemResponses = buildRentItemResponses(reservasi.getRentList());

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
                                .rentItems(rentItemResponses)
                                .createdAt(reservasi.getCreatedAt())
                                .updatedAt(reservasi.getUpdatedAt())
                                .build();
        }

        /**
         * Converts the flat rent UUID list into grouped RentItemResponse objects
         * with quantity and subtotal.
         */
        private List<RentItemResponse> buildRentItemResponses(List<UUID> rentList) {
                if (rentList == null || rentList.isEmpty())
                        return Collections.emptyList();

                Map<UUID, Long> counts = rentList.stream()
                                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

                List<RentItemResponse> responses = new ArrayList<>();
                for (Map.Entry<UUID, Long> entry : counts.entrySet()) {
                        AlatOlahraga equipment = alatOlahragaRepository.findById(entry.getKey())
                                        .orElse(null);
                        if (equipment != null) {
                                int qty = entry.getValue().intValue();
                                responses.add(RentItemResponse.builder()
                                                .alatOlahragaId(equipment.getId())
                                                .name(equipment.getName())
                                                .quantity(qty)
                                                .pricePerUnit(equipment.getPrice())
                                                .subtotal(equipment.getPrice() * qty)
                                                .build());
                        }
                }
                return responses;
        }
}
