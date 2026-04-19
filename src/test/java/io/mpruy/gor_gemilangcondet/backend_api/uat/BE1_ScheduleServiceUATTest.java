package io.mpruy.gor_gemilangcondet.backend_api.uat;

import io.mpruy.gor_gemilangcondet.backend_api.client.ReservasiClient;
import io.mpruy.gor_gemilangcondet.backend_api.dto.fadhil.FadhilCourtAvailabilityDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.fadhil.FadhilReservasiDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.fadhil.FadhilSlotDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.response.ScheduleResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.response.ScheduleTimeRowResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.bookings.Booking;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.Lapangan;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.LapanganStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.LapanganType;
import io.mpruy.gor_gemilangcondet.backend_api.enums.BookingStatus;
import io.mpruy.gor_gemilangcondet.backend_api.repository.BookingRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.LapanganRepository;
import io.mpruy.gor_gemilangcondet.backend_api.security.JwtRoleExtractor;
import io.mpruy.gor_gemilangcondet.backend_api.service.ScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UAT — PBI-BE1: WebSocket / Pembaruan Status Lapangan
 * Menguji perilaku ScheduleService sebagai unit terisolasi.
 *
 * Skenario yang dicakup:
 *   BE1-03 — Fallback ke data lokal saat Fadhil tidak tersedia → jadwal tetap tampil
 *   BE1-04 — Grid tidak berubah saat tidak ada transaksi (data stabil)
 *   BE2-03 — Payload update message memuat semua field yang wajib
 *   FE1-01 — Grid dimulai pukul 07:00 dan berakhir 22:00 (16 baris)
 *   FE1-03 — Admin melihat bookerName pada slot yang terisi
 *   FE1-04 — Guest/anonim tidak melihat bookerName
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BE1 · ScheduleService Unit UAT")
class BE1_ScheduleServiceUATTest {

    @Mock LapanganRepository        lapanganRepository;
    @Mock BookingRepository         bookingRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock ReservasiClient           reservasiClient;
    @Mock JwtRoleExtractor          jwtRoleExtractor;

    @InjectMocks ScheduleService scheduleService;

    private static final LocalDate TEST_DATE = LocalDate.of(2026, 3, 9);

    private static final UUID COURT1_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID COURT2_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private Lapangan court1;
    private Lapangan court2;

    @BeforeEach
    void setUp() {
        court1 = Lapangan.builder().id(COURT1_ID).name("Lapangan 1")
                .type(LapanganType.BADMINTON).status(LapanganStatus.TERSEDIA).tarifPerJam(50000).build();
        court2 = Lapangan.builder().id(COURT2_ID).name("Lapangan 2")
                .type(LapanganType.BADMINTON).status(LapanganStatus.TERSEDIA).tarifPerJam(50000).build();
    }

    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BE1-03 · Fallback ke DB lokal saat Fadhil tidak tersedia — jadwal tetap tampil")
    void be1_03_fallbackToLocalWhenFadhilUnavailable() {
        // Arrange: Fadhil returns empty (koneksi gagal)
        when(reservasiClient.getAvailability(TEST_DATE)).thenReturn(Collections.emptyList());
        when(jwtRoleExtractor.canViewBookerName(null)).thenReturn(false);
        when(lapanganRepository.findAll()).thenReturn(List.of(court1, court2));
        when(bookingRepository.findActiveByDate(eq(TEST_DATE), any())).thenReturn(Collections.emptyList());

        // Act
        ScheduleResponse response = scheduleService.getScheduleFromFadhil(TEST_DATE, null);

        // Assert: grid tetap muncul (fallback ke lokal)
        assertThat(response).isNotNull();
        assertThat(response.getTimeSlots()).isNotEmpty();
        assertThat(response.getDate()).isEqualTo(TEST_DATE);
    }

    @Test
    @DisplayName("BE1-04 · Grid stabil saat tidak ada transaksi (data tidak berubah sendiri)")
    void be1_04_gridStableWithoutAnyTransaction() {
        // Arrange: dua lapangan, tidak ada booking
        when(lapanganRepository.findAll()).thenReturn(List.of(court1, court2));
        when(bookingRepository.findActiveByDate(eq(TEST_DATE), any())).thenReturn(Collections.emptyList());

        // Act: panggil getSchedule dua kali, hasilnya harus identik
        ScheduleResponse first  = scheduleService.getSchedule(TEST_DATE);
        ScheduleResponse second = scheduleService.getSchedule(TEST_DATE);

        // Assert: jumlah baris dan slot sama
        assertThat(first.getTimeSlots()).hasSize(second.getTimeSlots().size());
        for (int i = 0; i < first.getTimeSlots().size(); i++) {
            assertThat(first.getTimeSlots().get(i).getTime())
                    .isEqualTo(second.getTimeSlots().get(i).getTime());
            assertThat(first.getTimeSlots().get(i).getSlots())
                    .hasSize(second.getTimeSlots().get(i).getSlots().size());
        }
    }

    @Test
    @DisplayName("FE1-01 · Grid dimulai pukul 07:00 dan berakhir 22:00 (16 baris waktu)")
    void fe1_01_gridHasSixteenTimeRowsFrom7To22() {
        // Arrange
        when(lapanganRepository.findAll()).thenReturn(List.of(court1));
        when(bookingRepository.findActiveByDate(eq(TEST_DATE), any())).thenReturn(Collections.emptyList());

        // Act
        ScheduleResponse response = scheduleService.getSchedule(TEST_DATE);

        // Assert: 16 baris (07:00, 08:00, ... 22:00)
        assertThat(response.getTimeSlots()).hasSize(16);
        assertThat(response.getTimeSlots().get(0).getTime()).isEqualTo("07:00");
        assertThat(response.getTimeSlots().get(15).getTime()).isEqualTo("22:00");
    }

    @Test
    @DisplayName("FE1-01 · Jumlah kolom per baris sesuai jumlah court")
    void fe1_01_eachTimeRowHasSlotCountMatchingCourts() {
        // Arrange: 6 lapangan
        List<Lapangan> sixCourts = List.of(
                Lapangan.builder().id(UUID.randomUUID()).name("L1").type(LapanganType.BADMINTON).status(LapanganStatus.TERSEDIA).tarifPerJam(50000).build(),
                Lapangan.builder().id(UUID.randomUUID()).name("L2").type(LapanganType.BADMINTON).status(LapanganStatus.TERSEDIA).tarifPerJam(50000).build(),
                Lapangan.builder().id(UUID.randomUUID()).name("L3").type(LapanganType.BADMINTON).status(LapanganStatus.TERSEDIA).tarifPerJam(50000).build(),
                Lapangan.builder().id(UUID.randomUUID()).name("L4").type(LapanganType.BADMINTON).status(LapanganStatus.TERSEDIA).tarifPerJam(50000).build(),
                Lapangan.builder().id(UUID.randomUUID()).name("L5").type(LapanganType.BADMINTON).status(LapanganStatus.TERSEDIA).tarifPerJam(50000).build(),
                Lapangan.builder().id(UUID.randomUUID()).name("L6").type(LapanganType.BADMINTON).status(LapanganStatus.TERSEDIA).tarifPerJam(50000).build()
        );
        when(lapanganRepository.findAll()).thenReturn(sixCourts);
        when(bookingRepository.findActiveByDate(eq(TEST_DATE), any())).thenReturn(Collections.emptyList());

        // Act
        ScheduleResponse response = scheduleService.getSchedule(TEST_DATE);

        // Assert: setiap baris punya 6 slot
        for (ScheduleTimeRowResponse row : response.getTimeSlots()) {
            assertThat(row.getSlots()).hasSize(6)
                    .withFailMessage("Baris %s seharusnya punya 6 slot", row.getTime());
        }
    }

    @Test
    @DisplayName("FE1-03 · Slot terisi dengan role ADMIN — bookerName tampil")
    void fe1_03_adminSeesBookerName() {
        // Arrange: slot jam 10:00 di lapangan 1 sudah dipesan oleh "Budi"
        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .lapangan(court1)
                .bookingDate(TEST_DATE)
                .startTime(LocalTime.of(10, 0))
                .customerName("Budi")
                .status(BookingStatus.CONFIRMED)
                .build();

        when(reservasiClient.getAvailability(TEST_DATE)).thenReturn(Collections.emptyList()); // fallback
        when(jwtRoleExtractor.canViewBookerName("ADMIN")).thenReturn(true); // admin bisa lihat bookerName
        when(lapanganRepository.findAll()).thenReturn(List.of(court1));
        when(bookingRepository.findActiveByDate(eq(TEST_DATE), any())).thenReturn(List.of(booking));

        // Act
        ScheduleResponse response = scheduleService.getScheduleFromFadhil(TEST_DATE, "ADMIN");

        // Assert: slot jam 10 menampilkan "Budi"
        var slot1000 = response.getTimeSlots().stream()
                .filter(r -> r.getTime().equals("10:00"))
                .flatMap(r -> r.getSlots().stream())
                .filter(s -> "BOOKED".equals(s.getStatus()))
                .findFirst();

        assertThat(slot1000).isPresent();
        assertThat(slot1000.get().getBookerName()).isEqualTo("Budi");
    }

    @Test
    @DisplayName("FE1-04 · Slot terisi tanpa JWT (guest / anonim) — bookerName null")
    void fe1_04_guestDoesNotSeeBookerName() {
        // Arrange: slot jam 10:00 sudah dipesan namun JWT tidak ada
        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .lapangan(court1)
                .bookingDate(TEST_DATE)
                .startTime(LocalTime.of(10, 0))
                .customerName("Rani")
                .status(BookingStatus.CONFIRMED)
                .build();

        when(reservasiClient.getAvailability(TEST_DATE)).thenReturn(Collections.emptyList()); // fallback
        when(jwtRoleExtractor.canViewBookerName(null)).thenReturn(false);
        when(lapanganRepository.findAll()).thenReturn(List.of(court1));
        when(bookingRepository.findActiveByDate(eq(TEST_DATE), any())).thenReturn(List.of(booking));

        // Act
        ScheduleResponse response = scheduleService.getScheduleFromFadhil(TEST_DATE, null);

        // Assert: bookerName null untuk semua slot
        response.getTimeSlots().stream()
                .flatMap(r -> r.getSlots().stream())
                .forEach(slot -> assertThat(slot.getBookerName())
                        .withFailMessage("Guest tidak boleh melihat bookerName, tapi slot %s menampilkan: %s",
                                slot.getCourtName(), slot.getBookerName())
                        .isNull());
    }

    @Test
    @DisplayName("FE2-02 · Slot tersedia memiliki status AVAILABLE dan label yang sesuai")
    void fe2_02_availableSlotHasCorrectStatus() {
        // Arrange: tidak ada booking
        when(lapanganRepository.findAll()).thenReturn(List.of(court1));
        when(bookingRepository.findActiveByDate(eq(TEST_DATE), any())).thenReturn(Collections.emptyList());

        // Act
        ScheduleResponse response = scheduleService.getSchedule(TEST_DATE);

        // Assert: semua slot berstatus AVAILABLE
        long available = response.getTimeSlots().stream()
                .flatMap(r -> r.getSlots().stream())
                .filter(s -> "AVAILABLE".equals(s.getStatus()))
                .count();
        long total = response.getTimeSlots().stream()
                .mapToLong(r -> r.getSlots().size())
                .sum();

        assertThat(available).isEqualTo(total)
                .withFailMessage("Semua slot seharusnya AVAILABLE saat tidak ada booking");
    }

    @Test
    @DisplayName("BE2-03 · buildUpdateMessage menghasilkan payload lengkap")
    void be2_03_updateMessageHasAllRequiredFields() {
        // Arrange
        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .lapangan(court1)
                .bookingDate(TEST_DATE)
                .startTime(LocalTime.of(14, 0))
                .customerName("Ahmad")
                .status(BookingStatus.CONFIRMED)
                .build();

        // Act
        var msg = scheduleService.buildUpdateMessage(booking);

        // Assert: semua field wajib ada
        assertThat(msg.getDate()).isEqualTo("2026-03-09");
        assertThat(msg.getLapanganId()).isEqualTo(COURT1_ID.toString());
        assertThat(msg.getCourtName()).isEqualTo("Lapangan 1");
        assertThat(msg.getTime()).isEqualTo("14:00");
        assertThat(msg.getStatus()).isEqualTo("BOOKED");
        assertThat(msg.getBookerName()).isEqualTo("Ahmad");
        assertThat(msg.getLastUpdated()).isNotNull();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // FADHIL PATH — tests exercising getScheduleFromFadhil when Fadhil is online
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("FADHIL · Guest melihat slot AVAILABLE dan BOOKED dari data Fadhil (tanpa bookerName)")
    void fadhil_guestBuildsGridWithCorrectStatuses() {
        UUID lapanganId = UUID.randomUUID();

        FadhilSlotDto availSlot = new FadhilSlotDto();
        availSlot.setStartHour(9);
        availSlot.setAvailable(true);

        FadhilSlotDto bookedSlot = new FadhilSlotDto();
        bookedSlot.setStartHour(10);
        bookedSlot.setAvailable(false);

        FadhilCourtAvailabilityDto court = new FadhilCourtAvailabilityDto();
        court.setLapanganId(lapanganId);
        court.setLapanganName("Lapangan B");
        court.setSlots(List.of(availSlot, bookedSlot));

        when(jwtRoleExtractor.canViewBookerName(null)).thenReturn(false);
        when(reservasiClient.getAvailability(TEST_DATE)).thenReturn(List.of(court));

        ScheduleResponse response = scheduleService.getScheduleFromFadhil(TEST_DATE, null);

        assertThat(response).isNotNull();
        assertThat(response.getDate()).isEqualTo(TEST_DATE);

        long available = response.getTimeSlots().stream()
                .flatMap(r -> r.getSlots().stream())
                .filter(s -> "AVAILABLE".equals(s.getStatus()))
                .count();
        assertThat(available).isGreaterThan(0);

        // Guest tidak boleh lihat bookerName
        response.getTimeSlots().stream()
                .flatMap(r -> r.getSlots().stream())
                .filter(s -> "BOOKED".equals(s.getStatus()))
                .forEach(s -> assertThat(s.getBookerName()).isNull());
    }

    @Test
    @DisplayName("FADHIL · Admin melihat bookerName via data reservasi Fadhil")
    void fadhil_adminSeesBookerNameFromFadhilReservations() {
        UUID lapanganId = UUID.randomUUID();

        FadhilSlotDto bookedSlot = new FadhilSlotDto();
        bookedSlot.setStartHour(10);
        bookedSlot.setAvailable(false);

        FadhilCourtAvailabilityDto court = new FadhilCourtAvailabilityDto();
        court.setLapanganId(lapanganId);
        court.setLapanganName("Lapangan A");
        court.setSlots(List.of(bookedSlot));

        FadhilReservasiDto reservasi = new FadhilReservasiDto();
        reservasi.setLapanganId(lapanganId);
        reservasi.setNamaWakil("Budi Santoso");
        reservasi.setReservationStart(LocalDateTime.of(2026, 3, 9, 10, 0));
        reservasi.setReservationEnd(LocalDateTime.of(2026, 3, 9, 11, 0));

        when(jwtRoleExtractor.canViewBookerName("ADMIN")).thenReturn(true);
        when(reservasiClient.getAvailability(TEST_DATE)).thenReturn(List.of(court));
        when(reservasiClient.getAllReservations()).thenReturn(List.of(reservasi));

        ScheduleResponse response = scheduleService.getScheduleFromFadhil(TEST_DATE, "ADMIN");

        var bookedSlots = response.getTimeSlots().stream()
                .flatMap(r -> r.getSlots().stream())
                .filter(s -> "BOOKED".equals(s.getStatus()))
                .toList();

        assertThat(bookedSlots).isNotEmpty();
        assertThat(bookedSlots.get(0).getBookerName()).isEqualTo("Budi Santoso");
    }

    @Test
    @DisplayName("FADHIL · Lapangan dengan slot null dilewati tanpa error; lapangan lain tetap tampil")
    void fadhil_courtWithNullSlotsIsSkippedGracefully() {
        FadhilCourtAvailabilityDto courtNull = new FadhilCourtAvailabilityDto();
        courtNull.setLapanganId(UUID.randomUUID());
        courtNull.setLapanganName("Lapangan C");
        courtNull.setSlots(null); // edge case: slot null

        FadhilSlotDto slot = new FadhilSlotDto();
        slot.setStartHour(8);
        slot.setAvailable(true);

        FadhilCourtAvailabilityDto courtValid = new FadhilCourtAvailabilityDto();
        courtValid.setLapanganId(UUID.randomUUID());
        courtValid.setLapanganName("Lapangan D");
        courtValid.setSlots(List.of(slot));

        when(jwtRoleExtractor.canViewBookerName(null)).thenReturn(false);
        when(reservasiClient.getAvailability(TEST_DATE)).thenReturn(List.of(courtNull, courtValid));

        ScheduleResponse response = scheduleService.getScheduleFromFadhil(TEST_DATE, null);

        assertThat(response).isNotNull();
        assertThat(response.getTimeSlots()).isNotEmpty();
    }

    /* Helper: build FadhilSlotDto */
    private FadhilSlotDto availableSlot(int hour) {
        FadhilSlotDto s = new FadhilSlotDto();
        s.setStartHour(hour);
        s.setAvailable(true);
        return s;
    }
}
