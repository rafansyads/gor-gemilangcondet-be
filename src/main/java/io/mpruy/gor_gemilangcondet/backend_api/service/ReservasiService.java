package io.mpruy.gor_gemilangcondet.backend_api.service;

import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.requests.*;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.BatchReservasiResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.*;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.*;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.alat_olahraga.AlatOlahraga;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.alat_olahraga.AlatOlahragaStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.alat_olahraga.AlatOlahragaType;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.LapanganLog;
import io.mpruy.gor_gemilangcondet.backend_api.exception.BadRequestException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ResourceNotFoundException;
import io.mpruy.gor_gemilangcondet.backend_api.repository.AlatOlahragaRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.LapanganLogRepository;
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
        private final LapanganLogRepository lapanganLogRepository;
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
        private static final Map<LapanganType, List<AlatOlahragaType>> COURT_EQUIPMENT_MAP = Map.of(
                        LapanganType.BADMINTON, List.of(AlatOlahragaType.RAKET));

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

        @Transactional(readOnly = true)
        public LapanganResponse getCourt(UUID id) {
                Lapangan lapangan = lapanganRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Lapangan tidak ditemukan: " + id));
                return toLapanganResponse(lapangan);
        }

        @Transactional(readOnly = true)
        public List<LapanganLogResponse> getCourtLogs(UUID id) {
                if (!lapanganRepository.existsById(id)) {
                        throw new ResourceNotFoundException("Lapangan tidak ditemukan: " + id);
                }
                return lapanganLogRepository.findByLapanganIdOrderByCreatedAtDesc(id).stream()
                                .map(this::toLapanganLogResponse)
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

                // Determine the first bookable slot (next full hour if today).
                // Past slots are still returned but marked as available=false.
                LocalDateTime now = LocalDateTime.now(ZONE_JAKARTA);
                int firstBookableHour = OPENING_HOUR;

                if (date.equals(now.toLocalDate())) {
                        firstBookableHour = now.getHour() + 1;
                        if (firstBookableHour < OPENING_HOUR)
                                firstBookableHour = OPENING_HOUR;
                        // Do NOT return early — we still show all past slots as unavailable
                }

                List<CourtAvailabilityResponse> result = new ArrayList<>();

                for (Lapangan court : courts) {
                        // Courts under maintenance are fully unavailable
                        if (court.getStatus() == LapanganStatus.DALAM_PERBAIKAN) {
                                List<SlotAvailabilityResponse> allUnavailable = new ArrayList<>();
                                for (int h = OPENING_HOUR; h < CLOSING_HOUR; h++) {
                                        allUnavailable.add(SlotAvailabilityResponse.builder()
                                                        .startHour(h).endHour(h + 1).available(false).build());
                                }
                                result.add(CourtAvailabilityResponse.builder()
                                                .lapanganId(court.getId())
                                                .lapanganName(court.getName())
                                                .lapanganType(court.getType())
                                                .status(court.getStatus())
                                                .tarifPerJam(court.getTarifPerJam())
                                                .date(date)
                                                .imageUrl(court.getImageUrl())
                                                .slots(allUnavailable)
                                                .build());
                                continue;
                        }

                        // Find active reservations for this court on this date
                        List<Reservasi> reservations = reservasiRepository.findOverlappingReservations(
                                        court.getId(), dayStart, dayEnd, INACTIVE_STATUSES);

                        List<SlotAvailabilityResponse> slots = new ArrayList<>();
                        for (int h = OPENING_HOUR; h < CLOSING_HOUR; h++) {
                                LocalDateTime slotStart = date.atTime(h, 0);
                                LocalDateTime slotEnd = date.atTime(h + 1, 0);

                                // Past slots (for today) are always unavailable for booking
                                if (h < firstBookableHour) {
                                        slots.add(SlotAvailabilityResponse.builder()
                                                        .startHour(h).endHour(h + 1).available(false).build());
                                        continue;
                                }

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
                                        .status(court.getStatus())
                                        .tarifPerJam(court.getTarifPerJam())
                                        .date(date)
                                        .imageUrl(court.getImageUrl())
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

                List<AlatOlahragaType> relevantTypes = COURT_EQUIPMENT_MAP.getOrDefault(
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

        /**
         * Reschedules an entire batch of reservations by re-allocating them into 1-hour slots.
         * This allows splitting a 2-hour contiguous block into separate 1-hour slots.
         */
        @Transactional
        public BatchReservasiResponse rescheduleBatch(UUID batchId, RescheduleBatchRequest request) {
                LocalDateTime now = LocalDateTime.now(ZONE_JAKARTA);

                // 1. Get all reservations belonging to this batch
                List<Reservasi> batchReservations = reservasiRepository.findByBatchId(batchId);
                if (batchReservations.isEmpty()) {
                        throw new ResourceNotFoundException("Batch reservasi tidak ditemukan: " + batchId);
                }

                // 2. Calculate total Duration (to ensure parity)
                int totalOldDuration = batchReservations.stream()
                                .mapToInt(r -> (int) ChronoUnit.HOURS.between(r.getReservationStart(), r.getReservationEnd()))
                                .sum();

                if (request.getNewStarts().size() != totalOldDuration) {
                        throw new BadRequestException("Jumlah jam baru (" + request.getNewStarts().size() + 
                                        ") harus sama dengan jumlah jam pesanan awal (" + totalOldDuration + ")");
                }

                // 2b. Validate consecutive-group pattern is preserved
                // Expand each reservation into individual hours (e.g. 13:00-15:00 → [13, 14])
                List<Integer> oldHours = batchReservations.stream()
                                .flatMapToInt(r -> {
                                        int start = r.getReservationStart().getHour();
                                        int duration = (int) ChronoUnit.HOURS.between(r.getReservationStart(), r.getReservationEnd());
                                        return java.util.stream.IntStream.range(start, start + duration);
                                })
                                .boxed()
                                .collect(Collectors.toList());
                List<Integer> oldGroupSizes = computeConsecutiveGroupSizes(oldHours);
                List<Integer> newGroupSizes = computeConsecutiveGroupSizes(
                                request.getNewStarts().stream()
                                                .map(LocalDateTime::getHour)
                                                .collect(Collectors.toList()));

                Collections.sort(oldGroupSizes);
                Collections.sort(newGroupSizes);
                if (!oldGroupSizes.equals(newGroupSizes)) {
                        throw new BadRequestException(
                                        "Pola slot harus sama dengan pesanan awal. "
                                        + "Slot berurutan harus tetap berurutan. "
                                        + "Contoh: jika awalnya 2 jam berurutan + 1 jam terpisah, "
                                        + "maka jadwal baru juga harus 2 jam berurutan + 1 jam terpisah.");
                }

                // 3. Prepare common context from the primary (first) reservation
                Reservasi primary = batchReservations.get(0);
                UUID userId = primary.getUserId();
                UUID paymentId = primary.getPaymentId();
                String namaWakil = primary.getNamaWakil();
                String nomorTelepon = primary.getNomorTelepon();
                int jumlahOrang = primary.getJumlahOrang();
                ReservasiStatus status = primary.getStatus();
                LocalDateTime paymentDeadline = primary.getPaymentDeadline();
                String paymentProofUrl = primary.getPaymentProofUrl();
                
                // Aggregate ALL rent items from the entire old batch
                List<UUID> allOldRentItems = batchReservations.stream()
                                .filter(r -> r.getRentList() != null)
                                .flatMap(r -> r.getRentList().stream())
                                .collect(Collectors.toList());

                // 4. Validate court and lock
                UUID targetCourtId = (request.getNewLapanganId() != null)
                                ? request.getNewLapanganId()
                                : primary.getLapangan().getId();

                Lapangan targetLapangan = lapanganRepository.findByIdWithPessimisticLock(targetCourtId)
                                .orElseThrow(() -> new ResourceNotFoundException("Lapangan tidak ditemukan: " + targetCourtId));

                if (targetLapangan.getStatus() == LapanganStatus.DALAM_PERBAIKAN) {
                        throw new BadRequestException("Lapangan '" + targetLapangan.getName() + "' sedang dalam perbaikan");
                }
                if (targetLapangan.getType() != primary.getLapangan().getType()) {
                        throw new BadRequestException("Tipe lapangan tidak kompatibel");
                }

                // 5. Validation and overlap check for EACH new 1-hour slot
                for (LocalDateTime newStart : request.getNewStarts()) {
                        if (newStart.isBefore(now)) {
                                throw new BadRequestException("Waktu mulai baru tidak boleh di masa lalu: " + newStart);
                        }
                        if (newStart.getMinute() != 0) {
                                throw new BadRequestException("Waktu harus pada jam tepat: " + newStart);
                        }
                        int h = newStart.getHour();
                        if (h < OPENING_HOUR || h >= CLOSING_HOUR) {
                                throw new BadRequestException("Di luar jam operasional GOR: " + newStart);
                        }

                        // Overlap (excluding the current batch)
                        List<Reservasi> overlapping = reservasiRepository.findOverlappingReservations(
                                        targetCourtId, newStart, newStart.plusHours(1), INACTIVE_STATUSES);
                        overlapping.removeIf(r -> batchId.equals(r.getBatchId()));
                        if (!overlapping.isEmpty()) {
                                throw new BadRequestException("Slot jam " + h + ":00 sudah terisi.");
                        }
                }

                // Internal overlap check within the request (e.g. duplicate start times)
                long uniqueStarts = request.getNewStarts().stream().distinct().count();
                if (uniqueStarts != request.getNewStarts().size()) {
                        throw new BadRequestException("Ada slot jam yang duplikat dalam pilihan baru Anda.");
                }

                // 6. DELETE old reservations and CREATE new 1-hour entities
                reservasiRepository.deleteAll(batchReservations);

                List<ReservasiResponse> updatedResponses = new ArrayList<>();
                double totalBatchCost = 0;
                List<UUID> remainingRentItems = new ArrayList<>(allOldRentItems);

                for (int i = 0; i < request.getNewStarts().size(); i++) {
                        LocalDateTime newStart = request.getNewStarts().get(i);
                        double courtCost = targetLapangan.getTarifPerJam();
                        
                        // Distribute one item to each slot until we run out
                        List<UUID> currentRentList = new ArrayList<>();
                        if (!remainingRentItems.isEmpty()) {
                                currentRentList.add(remainingRentItems.remove(0));
                        }
                        
                        // Special case: if this is the LAST slot but we still have many items left,
                        // put all remaining items here to ensure no data loss.
                        if (i == (request.getNewStarts().size() - 1) && !remainingRentItems.isEmpty()) {
                                currentRentList.addAll(remainingRentItems);
                                remainingRentItems.clear();
                        }

                        Reservasi newRes = Reservasi.builder()
                                        .userId(userId)
                                        .paymentId(paymentId)
                                        .batchId(batchId)
                                        .lapangan(targetLapangan)
                                        .reservationStart(newStart)
                                        .reservationEnd(newStart.plusHours(1))
                                        .status(status)
                                        .namaWakil(namaWakil)
                                        .nomorTelepon(nomorTelepon)
                                        .jumlahOrang(jumlahOrang)
                                        .paymentProofUrl(paymentProofUrl)
                                        .totalPayment(courtCost + calculateExistingEquipmentCost(currentRentList))
                                        .rentList(currentRentList)
                                        .paymentDeadline(paymentDeadline)
                                        .createdAt(primary.getCreatedAt())
                                        .updatedAt(now)
                                        .build();

                        reservasiRepository.save(newRes);
                        updatedResponses.add(toReservasiResponse(newRes));
                        totalBatchCost += newRes.getTotalPayment();
                }

                return BatchReservasiResponse.builder()
                                .batchId(batchId)
                                .reservations(updatedResponses)
                                .totalPayment(totalBatchCost)
                                .primaryReservationId(updatedResponses.get(0).getId())
                                .build();
        }

        // ──────────────────────────────────────────────────────────────────────────
        // Court CRUD
        // ──────────────────────────────────────────────────────────────────────────

        @Transactional
        public LapanganResponse createCourt(CreateLapanganRequest request) {
                if (lapanganRepository.existsByKode(request.getKode())) {
                        throw new BadRequestException(
                                        "Kode lapangan '" + request.getKode() + "' sudah digunakan");
                }
                LocalDateTime now = LocalDateTime.now(ZONE_JAKARTA);
                Lapangan lapangan = Lapangan.builder()
                                .name(request.getName())
                                .kode(request.getKode())
                                .type(request.getType())
                                .jenisLantai(request.getJenisLantai())
                                .fasilitas(request.getFasilitas() != null ? request.getFasilitas() : List.of())
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
                lapangan.setJenisLantai(request.getJenisLantai());
                if (request.getFasilitas() != null) {
                        lapangan.setFasilitas(request.getFasilitas());
                }
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

        @Transactional
        public LapanganResponse uploadCourtImage(UUID id, org.springframework.web.multipart.MultipartFile file) {
                Lapangan lapangan = lapanganRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Lapangan tidak ditemukan: " + id));
                try {
                        String originalFilename = file.getOriginalFilename();
                        String ext = (originalFilename != null && originalFilename.contains("."))
                                        ? originalFilename.substring(originalFilename.lastIndexOf('.'))
                                        : ".jpg";
                        String filename = "court-" + id + "-" + System.currentTimeMillis() + ext;
                        java.nio.file.Path uploadPath = java.nio.file.Paths.get("uploads/court-images");
                        java.nio.file.Files.createDirectories(uploadPath);
                        java.nio.file.Path filePath = uploadPath.resolve(filename);
                        java.nio.file.Files.copy(file.getInputStream(), filePath,
                                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        lapangan.setImageUrl(filename);
                        lapangan.setUpdatedAt(LocalDateTime.now(ZONE_JAKARTA));
                        lapanganRepository.save(lapangan);
                } catch (Exception e) {
                        throw new BadRequestException("Gagal mengunggah gambar: " + e.getMessage());
                }
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

        // ──────────────────────────────────────────────────────────────────────────
        // Get Batch Reservations
        // ──────────────────────────────────────────────────────────────────────────

        @Transactional(readOnly = true)
        public List<ReservasiResponse> getBatchByBatchId(UUID batchId) {
                return reservasiRepository.findByBatchId(batchId).stream()
                                .map(this::toReservasiResponse)
                                .collect(Collectors.toList());
        }

        // ──────────────────────────────────────────────────────────────────────────
        // Create Batch Reservation (multiple non-consecutive slots, one transaction)
        // ──────────────────────────────────────────────────────────────────────────

        /**
         * Creates multiple reservations (for non-consecutive time slots) under a single
         * batch ID, so they can be paid together and tracked as one transaction.
         * Each reservation is validated and created atomically. If any slot is
         * already taken the whole batch fails (transaction rollback).
         */
        @Transactional
        public BatchReservasiResponse createBatchReservation(CreateBatchReservasiRequest request) {
                UUID sharedBatchId = UUID.randomUUID();
                List<ReservasiResponse> responses = new ArrayList<>();
                double total = 0;

                for (CreateReservasiRequest req : request.getReservations()) {
                        ReservasiResponse r = createReservationWithBatchId(req, sharedBatchId);
                        responses.add(r);
                        total += r.getTotalPayment();
                }

                return BatchReservasiResponse.builder()
                                .batchId(sharedBatchId)
                                .reservations(responses)
                                .totalPayment(total)
                                .primaryReservationId(responses.get(0).getId())
                                .build();
        }

        /**
         * Internal variant of {@link #createReservation} that stamps a {@code batchId}
         * onto the saved entity. Shares all validation/locking logic.
         */
        private ReservasiResponse createReservationWithBatchId(CreateReservasiRequest request, UUID batchId) {
                LocalDateTime now = LocalDateTime.now(ZONE_JAKARTA);

                if (request.getReservationStart().isBefore(now)) {
                        throw new BadRequestException("Tanggal reservasi tidak boleh di masa lalu");
                }
                if (request.getReservationStart().getMinute() != 0 ||
                                request.getReservationStart().getSecond() != 0) {
                        throw new BadRequestException(
                                        "Waktu reservasi harus dimulai pada jam tepat (contoh: 08:00, 09:00)");
                }
                int startHour = request.getReservationStart().getHour();
                int endHour = startHour + request.getDurationInHours();
                if (startHour < OPENING_HOUR || endHour > CLOSING_HOUR) {
                        throw new BadRequestException(
                                        String.format("Jam operasional GOR: %02d:00 - %02d:00", OPENING_HOUR, CLOSING_HOUR));
                }

                Lapangan lapangan = lapanganRepository.findByIdWithPessimisticLock(request.getLapanganId())
                                .orElseThrow(() -> new ResourceNotFoundException("Lapangan tidak ditemukan"));
                if (lapangan.getStatus() != LapanganStatus.TERSEDIA) {
                        throw new BadRequestException(
                                        "Lapangan sedang tidak tersedia (status: " + lapangan.getStatus() + ")");
                }

                LocalDateTime reservationEnd = request.getReservationStart()
                                .plusHours(request.getDurationInHours());

                if (lapangan.getMaintenanceStart() != null && lapangan.getMaintenanceEnd() != null) {
                        if (lapangan.getMaintenanceStart().isBefore(reservationEnd) &&
                                        lapangan.getMaintenanceEnd().isAfter(request.getReservationStart())) {
                                throw new BadRequestException(
                                                "Lapangan sedang dalam perbaikan pada waktu yang dipilih");
                        }
                }

                List<Reservasi> overlapping = reservasiRepository.findOverlappingReservations(
                                lapangan.getId(), request.getReservationStart(), reservationEnd, INACTIVE_STATUSES);
                if (!overlapping.isEmpty()) {
                        throw new BadRequestException("Jadwal lapangan sudah terisi pada waktu yang dipilih");
                }

                List<UUID> expandedRentList = new ArrayList<>();
                double equipmentCost = 0;
                if (request.getRentItems() != null && !request.getRentItems().isEmpty()) {
                        equipmentCost = processRentItems(
                                        request.getRentItems(),
                                        request.getReservationStart(),
                                        reservationEnd,
                                        expandedRentList);
                }

                double totalCost = lapangan.getTarifPerJam() * request.getDurationInHours() + equipmentCost;

                Reservasi reservasi = Reservasi.builder()
                                .batchId(batchId)
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

        private LapanganLogResponse toLapanganLogResponse(LapanganLog log) {
                return LapanganLogResponse.builder()
                                .id(log.getId())
                                .lapanganId(log.getLapanganId())
                                .userId(log.getUserId())
                                .username(log.getUsername())
                                .changeDescription(log.getChangeDescription())
                                .createdAt(log.getCreatedAt())
                                .build();
        }

        private LapanganResponse toLapanganResponse(Lapangan lapangan) {
                return LapanganResponse.builder()
                                .id(lapangan.getId())
                                .name(lapangan.getName())
                                .kode(lapangan.getKode())
                                .type(lapangan.getType())
                                .status(lapangan.getStatus())
                                .jenisLantai(lapangan.getJenisLantai())
                                .fasilitas(lapangan.getFasilitas())
                                .tarifPerJam(lapangan.getTarifPerJam())
                                .imageUrl(lapangan.getImageUrl())
                                .build();
        }

        private ReservasiResponse toReservasiResponse(Reservasi reservasi) {
                List<RentItemResponse> rentItemResponses = buildRentItemResponses(reservasi.getRentList());

                int duration = (int) ChronoUnit.HOURS.between(
                                reservasi.getReservationStart(), reservasi.getReservationEnd());

                return ReservasiResponse.builder()
                                .id(reservasi.getId())
                                .userId(reservasi.getUserId())
                                .batchId(reservasi.getBatchId())
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

        /**
         * Groups a list of hours into consecutive runs and returns the sizes.
         * E.g. [9, 10, 13] → groups [9,10] and [13] → sizes [2, 1]
         */
        private List<Integer> computeConsecutiveGroupSizes(List<Integer> hours) {
                if (hours.isEmpty()) return List.of();
                List<Integer> sorted = hours.stream().sorted().collect(Collectors.toList());
                List<Integer> groupSizes = new ArrayList<>();
                int groupSize = 1;
                for (int i = 1; i < sorted.size(); i++) {
                        if (sorted.get(i) == sorted.get(i - 1) + 1) {
                                groupSize++;
                        } else {
                                groupSizes.add(groupSize);
                                groupSize = 1;
                        }
                }
                groupSizes.add(groupSize);
                return groupSizes;
        }
}
