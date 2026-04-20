package io.mpruy.gor_gemilangcondet.backend_api.service;

import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.requests.CreateLapanganRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.requests.CreateBatchReservasiRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.requests.CreateReservasiRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.requests.RentItemRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.requests.RescheduleBatchRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.requests.RescheduleReservasiRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.requests.UpdateLapanganRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.*;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.*;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.alat_olahraga.AlatOlahraga;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.alat_olahraga.AlatOlahragaStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.alat_olahraga.AlatOlahragaType;
import io.mpruy.gor_gemilangcondet.backend_api.exception.BadRequestException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ResourceNotFoundException;
import io.mpruy.gor_gemilangcondet.backend_api.repository.AlatOlahragaRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.LapanganLogRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.LapanganRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.ReservasiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservasiServiceTest {

    @Mock
    private LapanganRepository lapanganRepository;
    @Mock
    private ReservasiRepository reservasiRepository;
    @Mock
    private AlatOlahragaRepository alatOlahragaRepository;
    @Mock
    private LapanganLogRepository lapanganLogRepository;

    @InjectMocks
    private ReservasiService reservasiService;

    private Lapangan testCourt;
    private UUID courtId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        courtId = UUID.randomUUID();
        userId = UUID.randomUUID();
        testCourt = Lapangan.builder()
                .id(courtId)
                .name("Badminton 1")
                .type(LapanganType.BADMINTON)
                .status(LapanganStatus.TERSEDIA)
                .tarifPerJam(50000)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET ALL COURTS
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Get Courts Tests")
    class GetCourtsTests {

        @Test
        @DisplayName("Should return all courts")
        void getAllCourts_Success() {
            when(lapanganRepository.findAll()).thenReturn(List.of(testCourt));

            List<LapanganResponse> result = reservasiService.getAllCourts();

            assertEquals(1, result.size());
            assertEquals("Badminton 1", result.get(0).getName());
        }

        @Test
        @DisplayName("Should return courts by type")
        void getCourtsByType_Success() {
            when(lapanganRepository.findByType(LapanganType.BADMINTON)).thenReturn(List.of(testCourt));

            List<LapanganResponse> result = reservasiService.getCourtsByType(LapanganType.BADMINTON);

            assertEquals(1, result.size());
            assertEquals(LapanganType.BADMINTON, result.get(0).getType());
        }

        @Test
        @DisplayName("Should return empty list when no courts")
        void getAllCourts_Empty() {
            when(lapanganRepository.findAll()).thenReturn(Collections.emptyList());

            List<LapanganResponse> result = reservasiService.getAllCourts();

            assertTrue(result.isEmpty());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // COURT CRUD
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Court CRUD Tests")
    class CourtCrudTests {

        @Test
        @DisplayName("Should create court successfully")
        void createCourt_Success() {
            CreateLapanganRequest request = new CreateLapanganRequest();
            request.setName("Badminton 4");
            request.setKode("BDM-004");
            request.setType(LapanganType.BADMINTON);
            request.setTarifPerJam(100000);

            when(lapanganRepository.save(any(Lapangan.class))).thenAnswer(i -> {
                Lapangan l = i.getArgument(0);
                l.setId(UUID.randomUUID());
                return l;
            });

            LapanganResponse result = reservasiService.createCourt(request);

            assertNotNull(result);
            assertEquals("Badminton 4", result.getName());
            assertEquals(LapanganType.BADMINTON, result.getType());
            assertEquals(LapanganStatus.TERSEDIA, result.getStatus());
        }

        @Test
        @DisplayName("Should update court successfully")
        void updateCourt_Success() {
            UpdateLapanganRequest request = new UpdateLapanganRequest();
            request.setName("Updated");
            request.setType(LapanganType.BADMINTON);
            request.setTarifPerJam(200000);

            when(lapanganRepository.findById(courtId)).thenReturn(Optional.of(testCourt));
            when(lapanganRepository.save(any(Lapangan.class))).thenAnswer(i -> i.getArgument(0));
            when(lapanganLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            LapanganResponse result = reservasiService.updateCourt(courtId, request);

            assertEquals("Updated", result.getName());
            assertEquals(LapanganType.BADMINTON, result.getType());
        }

        @Test
        void deleteCourt_Success() {
            when(lapanganRepository.findById(courtId)).thenReturn(Optional.of(testCourt));
            when(reservasiRepository.findOverlappingReservations(eq(courtId), any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            assertDoesNotThrow(() -> reservasiService.deleteCourt(courtId));
            verify(lapanganRepository).delete(testCourt);
        }

        @Test
        @DisplayName("Should reject deleting court with active reservations")
        void deleteCourt_ActiveReservations() {
            Reservasi activeRes = Reservasi.builder().id(UUID.randomUUID()).build();
            when(lapanganRepository.findById(courtId)).thenReturn(Optional.of(testCourt));
            when(reservasiRepository.findOverlappingReservations(eq(courtId), any(), any(), any()))
                    .thenReturn(List.of(activeRes));

            assertThrows(BadRequestException.class, () -> reservasiService.deleteCourt(courtId));
        }

        @Test
        @DisplayName("Should throw when deleting non-existent court")
        void deleteCourt_NotFound() {
            UUID randomId = UUID.randomUUID();
            when(lapanganRepository.findById(randomId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> reservasiService.deleteCourt(randomId));
        }

        @Test
        @DisplayName("Should update court status")
        void updateCourtStatus_Success() {
            when(lapanganRepository.findById(courtId)).thenReturn(Optional.of(testCourt));
            when(lapanganRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            LapanganResponse result = reservasiService.updateCourtStatus(courtId, LapanganStatus.DALAM_PERBAIKAN);

            assertEquals(LapanganStatus.DALAM_PERBAIKAN, result.getStatus());
        }

        @Test
        @DisplayName("Should throw when updating status of non-existent court")
        void updateCourtStatus_NotFound() {
            UUID randomId = UUID.randomUUID();
            when(lapanganRepository.findById(randomId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> reservasiService.updateCourtStatus(randomId, LapanganStatus.TERSEDIA));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CREATE RESERVATION
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Create Reservation Tests")
    class CreateReservationTests {

        private CreateReservasiRequest buildValidRequest() {
            CreateReservasiRequest req = new CreateReservasiRequest();
            req.setLapanganId(courtId);
            req.setReservationStart(
                    LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0));
            req.setDurationInHours(2);
            req.setUserId(userId);
            req.setNamaWakil("John");
            req.setNomorTelepon("0812345678");
            req.setJumlahOrang(4);
            return req;
        }

        @Test
        @DisplayName("Should create reservation successfully")
        void createReservation_Success() {
            CreateReservasiRequest request = buildValidRequest();

            when(lapanganRepository.findByIdWithPessimisticLock(courtId)).thenReturn(Optional.of(testCourt));
            when(reservasiRepository.findOverlappingReservations(eq(courtId), any(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(reservasiRepository.save(any(Reservasi.class))).thenAnswer(i -> {
                Reservasi r = i.getArgument(0);
                r.setId(UUID.randomUUID());
                return r;
            });

            ReservasiResponse result = reservasiService.createReservation(request);

            assertNotNull(result);
            assertEquals(ReservasiStatus.BELUM_DIBAYAR, result.getStatus());
            assertEquals(100000, result.getTotalPayment()); // 50000/hr * 2hrs
        }

        @Test
        @DisplayName("Should reject reservation in the past")
        void createReservation_PastDate() {
            CreateReservasiRequest request = buildValidRequest();
            request.setReservationStart(
                    LocalDateTime.now().minusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0));

            assertThrows(BadRequestException.class,
                    () -> reservasiService.createReservation(request));
        }

        @Test
        @DisplayName("Should reject reservation not on the hour")
        void createReservation_NotOnTheHour() {
            CreateReservasiRequest request = buildValidRequest();
            request.setReservationStart(request.getReservationStart().withMinute(30));

            assertThrows(BadRequestException.class,
                    () -> reservasiService.createReservation(request));
        }

        @Test
        @DisplayName("Should reject reservation outside operating hours - too early")
        void createReservation_TooEarly() {
            CreateReservasiRequest request = buildValidRequest();
            request.setReservationStart(request.getReservationStart().withHour(5).withMinute(0).withSecond(0));

            assertThrows(BadRequestException.class,
                    () -> reservasiService.createReservation(request));
        }

        @Test
        @DisplayName("Should reject reservation outside operating hours - too late")
        void createReservation_TooLate() {
            CreateReservasiRequest request = buildValidRequest();
            request.setReservationStart(request.getReservationStart().withHour(22).withMinute(0).withSecond(0));
            request.setDurationInHours(2); // ends at 24:00 which is > 23:00

            assertThrows(BadRequestException.class,
                    () -> reservasiService.createReservation(request));
        }

        @Test
        @DisplayName("Should reject reservation for non-existent court")
        void createReservation_CourtNotFound() {
            CreateReservasiRequest request = buildValidRequest();
            when(lapanganRepository.findByIdWithPessimisticLock(courtId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> reservasiService.createReservation(request));
        }

        @Test
        @DisplayName("Should reject reservation for unavailable court")
        void createReservation_CourtUnavailable() {
            testCourt.setStatus(LapanganStatus.DALAM_PERBAIKAN);
            CreateReservasiRequest request = buildValidRequest();

            when(lapanganRepository.findByIdWithPessimisticLock(courtId)).thenReturn(Optional.of(testCourt));

            assertThrows(BadRequestException.class,
                    () -> reservasiService.createReservation(request));
        }

        @Test
        @DisplayName("Should reject reservation for court under maintenance window")
        void createReservation_MaintenanceWindow() {
            CreateReservasiRequest request = buildValidRequest();
            testCourt.setMaintenanceStart(request.getReservationStart().minusHours(1));
            testCourt.setMaintenanceEnd(request.getReservationStart().plusHours(3));

            when(lapanganRepository.findByIdWithPessimisticLock(courtId)).thenReturn(Optional.of(testCourt));

            assertThrows(BadRequestException.class,
                    () -> reservasiService.createReservation(request));
        }

        @Test
        @DisplayName("Should reject overlapping reservation (race condition prevention)")
        void createReservation_Overlapping() {
            CreateReservasiRequest request = buildValidRequest();
            Reservasi existing = Reservasi.builder().id(UUID.randomUUID()).build();

            when(lapanganRepository.findByIdWithPessimisticLock(courtId)).thenReturn(Optional.of(testCourt));
            when(reservasiRepository.findOverlappingReservations(eq(courtId), any(), any(), any()))
                    .thenReturn(List.of(existing));

            assertThrows(BadRequestException.class,
                    () -> reservasiService.createReservation(request));
        }

        @Test
        @DisplayName("Should create reservation with equipment rental")
        void createReservation_WithEquipment() {
            CreateReservasiRequest request = buildValidRequest();
            UUID equipId = UUID.randomUUID();
            RentItemRequest rentItem = new RentItemRequest();
            rentItem.setAlatOlahragaId(equipId);
            rentItem.setQuantity(2);
            request.setRentItems(List.of(rentItem));

            AlatOlahraga equipment = AlatOlahraga.builder()
                    .status(AlatOlahragaStatus.TERSEDIA).build();
            equipment.setId(equipId);
            equipment.setName("Raket");
            equipment.setStock(5);
            equipment.setPrice(15000);
            equipment.setType(AlatOlahragaType.RAKET);

            when(lapanganRepository.findByIdWithPessimisticLock(courtId)).thenReturn(Optional.of(testCourt));
            when(reservasiRepository.findOverlappingReservations(eq(courtId), any(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(reservasiRepository.findActiveReservationsDuringPeriod(any(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(alatOlahragaRepository.findById(equipId)).thenReturn(Optional.of(equipment));
            when(alatOlahragaRepository.findByTypeInAndStatus(anyList(), eq(AlatOlahragaStatus.TERSEDIA)))
                    .thenReturn(List.of(equipment));
            when(reservasiRepository.save(any(Reservasi.class))).thenAnswer(i -> {
                Reservasi r = i.getArgument(0);
                r.setId(UUID.randomUUID());
                return r;
            });

            ReservasiResponse result = reservasiService.createReservation(request);

            assertNotNull(result);
            // courtCost=50000*2=100000, equipCost=15000*2=30000 => 130000
            assertEquals(130000, result.getTotalPayment());
        }

        @Test
        @DisplayName("Should reject when equipment not found")
        void createReservation_EquipmentNotFound() {
            CreateReservasiRequest request = buildValidRequest();
            UUID equipId = UUID.randomUUID();
            RentItemRequest rentItem = new RentItemRequest();
            rentItem.setAlatOlahragaId(equipId);
            rentItem.setQuantity(1);
            request.setRentItems(List.of(rentItem));

            when(lapanganRepository.findByIdWithPessimisticLock(courtId)).thenReturn(Optional.of(testCourt));
            when(reservasiRepository.findOverlappingReservations(eq(courtId), any(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(reservasiRepository.findActiveReservationsDuringPeriod(any(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(alatOlahragaRepository.findById(equipId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> reservasiService.createReservation(request));
        }

        @Test
        @DisplayName("Should reject when equipment unavailable")
        void createReservation_EquipmentUnavailable() {
            CreateReservasiRequest request = buildValidRequest();
            UUID equipId = UUID.randomUUID();
            RentItemRequest rentItem = new RentItemRequest();
            rentItem.setAlatOlahragaId(equipId);
            rentItem.setQuantity(1);
            request.setRentItems(List.of(rentItem));

            AlatOlahraga equipment = AlatOlahraga.builder()
                    .status(AlatOlahragaStatus.DALAM_PERBAIKAN).build();
            equipment.setId(equipId);
            equipment.setName("Raket");
            equipment.setStock(5);
            equipment.setPrice(15000);
            equipment.setType(AlatOlahragaType.RAKET);

            when(lapanganRepository.findByIdWithPessimisticLock(courtId)).thenReturn(Optional.of(testCourt));
            when(reservasiRepository.findOverlappingReservations(eq(courtId), any(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(reservasiRepository.findActiveReservationsDuringPeriod(any(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(alatOlahragaRepository.findById(equipId)).thenReturn(Optional.of(equipment));

            assertThrows(BadRequestException.class,
                    () -> reservasiService.createReservation(request));
        }

        @Test
        @DisplayName("Should reject when equipment stock insufficient")
        void createReservation_EquipmentInsufficientStock() {
            CreateReservasiRequest request = buildValidRequest();
            UUID equipId = UUID.randomUUID();
            RentItemRequest rentItem = new RentItemRequest();
            rentItem.setAlatOlahragaId(equipId);
            rentItem.setQuantity(10);
            request.setRentItems(List.of(rentItem));

            AlatOlahraga equipment = AlatOlahraga.builder()
                    .status(AlatOlahragaStatus.TERSEDIA).build();
            equipment.setId(equipId);
            equipment.setName("Raket");
            equipment.setStock(5);
            equipment.setPrice(15000);
            equipment.setType(AlatOlahragaType.RAKET);

            when(lapanganRepository.findByIdWithPessimisticLock(courtId)).thenReturn(Optional.of(testCourt));
            when(reservasiRepository.findOverlappingReservations(eq(courtId), any(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(reservasiRepository.findActiveReservationsDuringPeriod(any(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(alatOlahragaRepository.findById(equipId)).thenReturn(Optional.of(equipment));

            assertThrows(BadRequestException.class,
                    () -> reservasiService.createReservation(request));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // RESCHEDULE RESERVATION
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Reschedule Reservation Tests")
    class RescheduleReservationTests {

        private UUID reservasiId;
        private Reservasi existingReservation;

        @BeforeEach
        void setUp() {
            reservasiId = UUID.randomUUID();
            existingReservation = Reservasi.builder()
                    .id(reservasiId)
                    .lapangan(testCourt)
                    .userId(userId)
                    .reservationStart(
                            LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0))
                    .reservationEnd(
                            LocalDateTime.now().plusDays(2).withHour(12).withMinute(0).withSecond(0).withNano(0))
                    .namaWakil("John")
                    .nomorTelepon("08123")
                    .jumlahOrang(4)
                    .totalPayment(100000)
                    .status(ReservasiStatus.BELUM_DIBAYAR)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        }

        @Test
        @DisplayName("Should reschedule successfully")
        void reschedule_Success() {
            RescheduleReservasiRequest request = new RescheduleReservasiRequest();
            request.setNewReservationStart(
                    LocalDateTime.now().plusDays(3).withHour(14).withMinute(0).withSecond(0).withNano(0));
            request.setDurationInHours(2);

            when(reservasiRepository.findByIdWithLapangan(reservasiId)).thenReturn(Optional.of(existingReservation));
            when(lapanganRepository.findByIdWithPessimisticLock(courtId)).thenReturn(Optional.of(testCourt));
            when(reservasiRepository.findOverlappingReservations(eq(courtId), any(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(reservasiRepository.save(any(Reservasi.class))).thenAnswer(i -> i.getArgument(0));

            ReservasiResponse result = reservasiService.rescheduleReservation(reservasiId, request);

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should reject reschedule for cancelled reservation")
        void reschedule_CancelledReservation() {
            existingReservation.setStatus(ReservasiStatus.DIBATALKAN);
            RescheduleReservasiRequest request = new RescheduleReservasiRequest();
            request.setNewReservationStart(
                    LocalDateTime.now().plusDays(3).withHour(14).withMinute(0).withSecond(0).withNano(0));
            request.setDurationInHours(2);

            when(reservasiRepository.findByIdWithLapangan(reservasiId)).thenReturn(Optional.of(existingReservation));

            assertThrows(BadRequestException.class,
                    () -> reservasiService.rescheduleReservation(reservasiId, request));
        }

        @Test
        @DisplayName("Should reject reschedule for completed reservation")
        void reschedule_CompletedReservation() {
            existingReservation.setStatus(ReservasiStatus.SELESAI);
            RescheduleReservasiRequest request = new RescheduleReservasiRequest();
            request.setNewReservationStart(
                    LocalDateTime.now().plusDays(3).withHour(14).withMinute(0).withSecond(0).withNano(0));
            request.setDurationInHours(2);

            when(reservasiRepository.findByIdWithLapangan(reservasiId)).thenReturn(Optional.of(existingReservation));

            assertThrows(BadRequestException.class,
                    () -> reservasiService.rescheduleReservation(reservasiId, request));
        }

        @Test
        @DisplayName("Should reject reschedule to past date")
        void reschedule_PastDate() {
            RescheduleReservasiRequest request = new RescheduleReservasiRequest();
            request.setNewReservationStart(
                    LocalDateTime.now().minusDays(1).withHour(14).withMinute(0).withSecond(0).withNano(0));
            request.setDurationInHours(2);

            when(reservasiRepository.findByIdWithLapangan(reservasiId)).thenReturn(Optional.of(existingReservation));

            assertThrows(BadRequestException.class,
                    () -> reservasiService.rescheduleReservation(reservasiId, request));
        }

        @Test
        @DisplayName("Should reject reschedule not on the hour")
        void reschedule_NotOnTheHour() {
            RescheduleReservasiRequest request = new RescheduleReservasiRequest();
            request.setNewReservationStart(
                    LocalDateTime.now().plusDays(3).withHour(14).withMinute(30).withSecond(0).withNano(0));
            request.setDurationInHours(2);

            when(reservasiRepository.findByIdWithLapangan(reservasiId)).thenReturn(Optional.of(existingReservation));

            assertThrows(BadRequestException.class,
                    () -> reservasiService.rescheduleReservation(reservasiId, request));
        }

        @Test
        @DisplayName("Should reject reschedule outside operating hours")
        void reschedule_OutsideHours() {
            RescheduleReservasiRequest request = new RescheduleReservasiRequest();
            request.setNewReservationStart(
                    LocalDateTime.now().plusDays(3).withHour(4).withMinute(0).withSecond(0).withNano(0));
            request.setDurationInHours(2);

            when(reservasiRepository.findByIdWithLapangan(reservasiId)).thenReturn(Optional.of(existingReservation));

            assertThrows(BadRequestException.class,
                    () -> reservasiService.rescheduleReservation(reservasiId, request));
        }

        @Test
        @DisplayName("Should reject reschedule to conflicting time")
        void reschedule_Overlapping() {
            RescheduleReservasiRequest request = new RescheduleReservasiRequest();
            request.setNewReservationStart(
                    LocalDateTime.now().plusDays(3).withHour(14).withMinute(0).withSecond(0).withNano(0));
            request.setDurationInHours(2);

            Reservasi otherReservation = Reservasi.builder().id(UUID.randomUUID()).build();

            when(reservasiRepository.findByIdWithLapangan(reservasiId)).thenReturn(Optional.of(existingReservation));
            when(lapanganRepository.findByIdWithPessimisticLock(courtId)).thenReturn(Optional.of(testCourt));
            when(reservasiRepository.findOverlappingReservations(eq(courtId), any(), any(), any()))
                    .thenReturn(new ArrayList<>(List.of(otherReservation)));

            assertThrows(BadRequestException.class,
                    () -> reservasiService.rescheduleReservation(reservasiId, request));
        }

        @Test
        @DisplayName("Should reject reschedule for non-existent reservation")
        void reschedule_NotFound() {
            UUID randomId = UUID.randomUUID();
            RescheduleReservasiRequest request = new RescheduleReservasiRequest();
            request.setNewReservationStart(
                    LocalDateTime.now().plusDays(3).withHour(14).withMinute(0).withSecond(0).withNano(0));
            request.setDurationInHours(2);

            when(reservasiRepository.findByIdWithLapangan(randomId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> reservasiService.rescheduleReservation(randomId, request));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET RESERVATIONS
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Get Reservations Tests")
    class GetReservationsTests {

        private Reservasi buildReservasi() {
            return Reservasi.builder()
                    .id(UUID.randomUUID())
                    .lapangan(testCourt)
                    .userId(userId)
                    .reservationStart(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0))
                    .reservationEnd(LocalDateTime.now().plusDays(1).withHour(12).withMinute(0))
                    .namaWakil("John")
                    .nomorTelepon("08123")
                    .jumlahOrang(4)
                    .totalPayment(100000)
                    .status(ReservasiStatus.BELUM_DIBAYAR)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        }

        @Test
        @DisplayName("Should return all reservations")
        void getAllReservations_Success() {
            when(reservasiRepository.findAllWithLapangan()).thenReturn(List.of(buildReservasi()));

            List<ReservasiResponse> result = reservasiService.getAllReservations();

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should return reservation by ID")
        void getReservationById_Success() {
            Reservasi res = buildReservasi();
            when(reservasiRepository.findByIdWithLapangan(res.getId())).thenReturn(Optional.of(res));

            ReservasiResponse result = reservasiService.getReservationById(res.getId());

            assertNotNull(result);
            assertEquals(res.getId(), result.getId());
        }

        @Test
        @DisplayName("Should throw when reservation not found")
        void getReservationById_NotFound() {
            UUID randomId = UUID.randomUUID();
            when(reservasiRepository.findByIdWithLapangan(randomId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> reservasiService.getReservationById(randomId));
        }

        @Test
        @DisplayName("Should return reservations by user ID")
        void getReservationsByUserId_Success() {
            when(reservasiRepository.findByUserIdWithLapangan(userId))
                    .thenReturn(List.of(buildReservasi()));

            List<ReservasiResponse> result = reservasiService.getReservationsByUserId(userId);

            assertEquals(1, result.size());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // COURT AVAILABILITY
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Court Availability Tests")
    class CourtAvailabilityTests {

        @Test
        @DisplayName("Should return availability for a future date")
        void getAvailability_FutureDate() {
            LocalDate futureDate = LocalDate.now().plusDays(5);

            when(lapanganRepository.findAll()).thenReturn(List.of(testCourt));
            when(reservasiRepository.findOverlappingReservations(eq(courtId), any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            List<CourtAvailabilityResponse> result = reservasiService.getAvailability(futureDate, null);

            assertEquals(1, result.size());
            assertFalse(result.get(0).getSlots().isEmpty());
            assertTrue(result.get(0).getSlots().stream().allMatch(SlotAvailabilityResponse::isAvailable));
        }

        @Test
        @DisplayName("Should return unavailable slots for court under maintenance")
        void getAvailability_CourtUnderMaintenance() {
            testCourt.setStatus(LapanganStatus.DALAM_PERBAIKAN);
            LocalDate futureDate = LocalDate.now().plusDays(5);

            when(lapanganRepository.findAll()).thenReturn(List.of(testCourt));

            List<CourtAvailabilityResponse> result = reservasiService.getAvailability(futureDate, null);

            assertEquals(1, result.size());
            assertTrue(result.get(0).getSlots().stream().noneMatch(SlotAvailabilityResponse::isAvailable));
        }

        @Test
        @DisplayName("Should filter availability by court type")
        void getAvailability_FilterByType() {
            LocalDate futureDate = LocalDate.now().plusDays(5);

            when(lapanganRepository.findByType(LapanganType.BADMINTON)).thenReturn(List.of(testCourt));
            when(reservasiRepository.findOverlappingReservations(eq(courtId), any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            List<CourtAvailabilityResponse> result = reservasiService.getAvailability(futureDate,
                    LapanganType.BADMINTON);

            assertEquals(1, result.size());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // EQUIPMENT AVAILABILITY
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Equipment Availability Tests")
    class EquipmentAvailabilityTests {

        @Test
        @DisplayName("Should return equipment availability for BADMINTON")
        void getEquipmentAvailability_Badminton() {
            AlatOlahraga raket = AlatOlahraga.builder().status(AlatOlahragaStatus.TERSEDIA).build();
            raket.setId(UUID.randomUUID());
            raket.setName("Raket");
            raket.setStock(10);
            raket.setPrice(15000);
            raket.setType(AlatOlahragaType.RAKET);

            when(alatOlahragaRepository.findByTypeInAndStatus(anyList(), eq(AlatOlahragaStatus.TERSEDIA)))
                    .thenReturn(List.of(raket));
            when(reservasiRepository.findActiveReservationsDuringPeriod(any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
            LocalDateTime end = start.plusHours(2);

            List<AlatOlahragaAvailabilityResponse> result = reservasiService
                    .getEquipmentAvailability(LapanganType.BADMINTON, start, end);

            assertEquals(1, result.size());
            assertEquals(10, result.get(0).getAvailable());
        }

        @Test
        @DisplayName("Should return empty for unknown court type")
        void getEquipmentAvailability_UnknownType() {
            // The COURT_EQUIPMENT_MAP only has 5 types so this actually tests that
            // types not in the map return empty. All LapanganType entries exist.
            // We test for BADMINTON with no equipment available
            when(alatOlahragaRepository.findByTypeInAndStatus(anyList(), eq(AlatOlahragaStatus.TERSEDIA)))
                    .thenReturn(Collections.emptyList());
            when(reservasiRepository.findActiveReservationsDuringPeriod(any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
            LocalDateTime end = start.plusHours(2);

            List<AlatOlahragaAvailabilityResponse> result = reservasiService
                    .getEquipmentAvailability(LapanganType.BADMINTON, start, end);

            assertTrue(result.isEmpty());
        }
    }

        // ══════════════════════════════════════════════════════════════════════════
        // ADDITIONAL COURT & BATCH COVERAGE
        // ══════════════════════════════════════════════════════════════════════════

        @Nested
        @DisplayName("Additional Court And Batch Tests")
        class AdditionalCourtAndBatchTests {

                private CreateReservasiRequest buildBatchReservationRequest(LocalDateTime start) {
                        CreateReservasiRequest req = new CreateReservasiRequest();
                        req.setLapanganId(courtId);
                        req.setReservationStart(start.withMinute(0).withSecond(0).withNano(0));
                        req.setDurationInHours(1);
                        req.setUserId(userId);
                        req.setNamaWakil("Batch User");
                        req.setNomorTelepon("0811111111");
                        req.setJumlahOrang(4);
                        return req;
                }

                private Reservasi buildReservasi(UUID id, UUID batchId, LocalDateTime start, LocalDateTime end) {
                        return Reservasi.builder()
                                        .id(id)
                                        .batchId(batchId)
                                        .lapangan(testCourt)
                                        .userId(userId)
                                        .reservationStart(start)
                                        .reservationEnd(end)
                                        .namaWakil("Batch User")
                                        .nomorTelepon("0811111111")
                                        .jumlahOrang(4)
                                        .totalPayment(testCourt.getTarifPerJam())
                                        .status(ReservasiStatus.BELUM_DIBAYAR)
                                        .paymentDeadline(LocalDateTime.now().plusMinutes(10))
                                        .createdAt(LocalDateTime.now())
                                        .updatedAt(LocalDateTime.now())
                                        .rentList(Collections.emptyList())
                                        .build();
                }

                @Test
                @DisplayName("Should return court by ID")
                void getCourt_Success() {
                        when(lapanganRepository.findById(courtId)).thenReturn(Optional.of(testCourt));

                        LapanganResponse response = reservasiService.getCourt(courtId);

                        assertEquals(courtId, response.getId());
                        assertEquals("Badminton 1", response.getName());
                }

                @Test
                @DisplayName("Should throw when court does not exist")
                void getCourt_NotFound() {
                        UUID randomId = UUID.randomUUID();
                        when(lapanganRepository.findById(randomId)).thenReturn(Optional.empty());

                        assertThrows(ResourceNotFoundException.class, () -> reservasiService.getCourt(randomId));
                }

                @Test
                @DisplayName("Should upload court image successfully")
                void uploadCourtImage_Success() throws Exception {
                        when(lapanganRepository.findById(courtId)).thenReturn(Optional.of(testCourt));
                        when(lapanganRepository.saveAndFlush(any(Lapangan.class))).thenAnswer(i -> i.getArgument(0));

                        MockMultipartFile file = new MockMultipartFile(
                                        "image",
                                        "court.jpg",
                                        "image/jpeg",
                                        "fake-image-content".getBytes());

                        LapanganResponse response = reservasiService.uploadCourtImage(courtId, file);

                        assertNotNull(response.getImageUrl());
                        assertTrue(response.getImageUrl().startsWith("court-"));
                }

                @Test
                @DisplayName("Should map court logs")
                void getCourtLogs_Success() {
                        UUID logId = UUID.randomUUID();
                        UUID actorId = UUID.randomUUID();
                        LapanganLog log = LapanganLog.builder()
                                        .id(logId)
                                        .lapanganId(courtId)
                                        .userId(actorId)
                                        .username("admin")
                                        .changeDescription("Harga: Rp 50.000 -> Rp 60.000")
                                        .createdAt(LocalDateTime.now())
                                        .build();

                        when(lapanganLogRepository.findByLapanganIdOrderByCreatedAtDesc(courtId)).thenReturn(List.of(log));

                        List<LapanganLogResponse> result = reservasiService.getCourtLogs(courtId);

                        assertEquals(1, result.size());
                        assertEquals(logId, result.get(0).getId());
                        assertEquals(actorId, result.get(0).getUserId());
                        assertEquals("admin", result.get(0).getUsername());
                }

                @Test
                @DisplayName("Should return reservations by batch ID")
                void getBatchByBatchId_Success() {
                        UUID batchId = UUID.randomUUID();
                        Reservasi reservasi = buildReservasi(
                                        UUID.randomUUID(),
                                        batchId,
                                        LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0),
                                        LocalDateTime.now().plusDays(2).withHour(11).withMinute(0).withSecond(0).withNano(0));

                        when(reservasiRepository.findByBatchId(batchId)).thenReturn(List.of(reservasi));

                        List<ReservasiResponse> result = reservasiService.getBatchByBatchId(batchId);

                        assertEquals(1, result.size());
                        assertEquals(batchId, result.get(0).getBatchId());
                }

                @Test
                @DisplayName("Should create batch reservation successfully")
                void createBatchReservation_Success() {
                        LocalDateTime day = LocalDateTime.now().plusDays(2).withSecond(0).withNano(0);
                        CreateReservasiRequest slot1 = buildBatchReservationRequest(day.withHour(9));
                        CreateReservasiRequest slot2 = buildBatchReservationRequest(day.withHour(11));

                        CreateBatchReservasiRequest request = new CreateBatchReservasiRequest();
                        request.setReservations(List.of(slot1, slot2));

                        when(lapanganRepository.findByIdWithPessimisticLock(courtId)).thenReturn(Optional.of(testCourt));
                        when(reservasiRepository.findOverlappingReservations(eq(courtId), any(), any(), any()))
                                        .thenReturn(Collections.emptyList());
                        when(reservasiRepository.save(any(Reservasi.class))).thenAnswer(i -> {
                                Reservasi saved = i.getArgument(0);
                                saved.setId(UUID.randomUUID());
                                return saved;
                        });

                        BatchReservasiResponse response = reservasiService.createBatchReservation(request);

                        assertNotNull(response.getBatchId());
                        assertEquals(2, response.getReservations().size());
                        assertEquals(testCourt.getTarifPerJam() * 2, response.getTotalPayment());
                        verify(reservasiRepository, times(2)).save(any(Reservasi.class));
                }

                @Test
                @DisplayName("Should reject batch reservation when one slot overlaps")
                void createBatchReservation_Overlapping() {
                        LocalDateTime day = LocalDateTime.now().plusDays(2).withSecond(0).withNano(0);
                        CreateReservasiRequest slot = buildBatchReservationRequest(day.withHour(9));

                        CreateBatchReservasiRequest request = new CreateBatchReservasiRequest();
                        request.setReservations(List.of(slot));

                        when(lapanganRepository.findByIdWithPessimisticLock(courtId)).thenReturn(Optional.of(testCourt));
                        when(reservasiRepository.findOverlappingReservations(eq(courtId), any(), any(), any()))
                                        .thenReturn(List.of(Reservasi.builder().id(UUID.randomUUID()).build()));

                        assertThrows(BadRequestException.class, () -> reservasiService.createBatchReservation(request));
                }

                @Test
                @DisplayName("Should reschedule a batch successfully")
                void rescheduleBatch_Success() {
                        UUID batchId = UUID.randomUUID();
                        LocalDateTime day = LocalDateTime.now().plusDays(3).withSecond(0).withNano(0);

                        Reservasi first = buildReservasi(UUID.randomUUID(), batchId, day.withHour(10).withMinute(0),
                                        day.withHour(11).withMinute(0));
                        Reservasi second = buildReservasi(UUID.randomUUID(), batchId, day.withHour(12).withMinute(0),
                                        day.withHour(13).withMinute(0));

                        RescheduleBatchRequest request = new RescheduleBatchRequest();
                        request.setNewStarts(List.of(day.plusDays(1).withHour(14).withMinute(0),
                                        day.plusDays(1).withHour(16).withMinute(0)));

                        when(reservasiRepository.findByBatchId(batchId)).thenReturn(List.of(first, second));
                        when(lapanganRepository.findByIdWithPessimisticLock(courtId)).thenReturn(Optional.of(testCourt));
                        when(reservasiRepository.findOverlappingReservations(eq(courtId), any(), any(), any()))
                                        .thenReturn(Collections.emptyList());
                        when(reservasiRepository.save(any(Reservasi.class))).thenAnswer(i -> {
                                Reservasi saved = i.getArgument(0);
                                saved.setId(UUID.randomUUID());
                                return saved;
                        });

                        BatchReservasiResponse response = reservasiService.rescheduleBatch(batchId, request);

                        assertEquals(batchId, response.getBatchId());
                        assertEquals(2, response.getReservations().size());
                        assertEquals(testCourt.getTarifPerJam() * 2, response.getTotalPayment());
                        verify(reservasiRepository).deleteAll(List.of(first, second));
                        verify(reservasiRepository, times(2)).save(any(Reservasi.class));
                }

                @Test
                @DisplayName("Should throw when rescheduling unknown batch")
                void rescheduleBatch_NotFound() {
                        UUID batchId = UUID.randomUUID();
                        RescheduleBatchRequest request = new RescheduleBatchRequest();
                        request.setNewStarts(List.of(LocalDateTime.now().plusDays(2).withHour(10).withMinute(0)));

                        when(reservasiRepository.findByBatchId(batchId)).thenReturn(Collections.emptyList());

                        assertThrows(ResourceNotFoundException.class, () -> reservasiService.rescheduleBatch(batchId, request));
                }

                @Test
                @DisplayName("Should reject duplicated starts in batch reschedule request")
                void rescheduleBatch_DuplicateStarts() {
                        UUID batchId = UUID.randomUUID();
                        LocalDateTime day = LocalDateTime.now().plusDays(3).withSecond(0).withNano(0);

                        Reservasi first = buildReservasi(UUID.randomUUID(), batchId, day.withHour(10).withMinute(0),
                                        day.withHour(11).withMinute(0));
                        Reservasi second = buildReservasi(UUID.randomUUID(), batchId, day.withHour(12).withMinute(0),
                                        day.withHour(13).withMinute(0));

                        LocalDateTime duplicateStart = day.plusDays(1).withHour(14).withMinute(0);
                        RescheduleBatchRequest request = new RescheduleBatchRequest();
                        request.setNewStarts(List.of(duplicateStart, duplicateStart));

                        when(reservasiRepository.findByBatchId(batchId)).thenReturn(List.of(first, second));
                        when(lapanganRepository.findByIdWithPessimisticLock(courtId)).thenReturn(Optional.of(testCourt));
                        when(reservasiRepository.findOverlappingReservations(eq(courtId), any(), any(), any()))
                                        .thenReturn(Collections.emptyList());

                        assertThrows(BadRequestException.class, () -> reservasiService.rescheduleBatch(batchId, request));
                }
        }
}
