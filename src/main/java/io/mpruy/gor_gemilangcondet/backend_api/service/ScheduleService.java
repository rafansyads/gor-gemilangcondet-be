package io.mpruy.gor_gemilangcondet.backend_api.service;

import io.mpruy.gor_gemilangcondet.backend_api.client.ReservasiClient;
import io.mpruy.gor_gemilangcondet.backend_api.dto.fadhil.FadhilCourtAvailabilityDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.fadhil.FadhilReservasiDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.fadhil.FadhilSlotDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.message.ScheduleUpdateMessage;
import io.mpruy.gor_gemilangcondet.backend_api.dto.response.ScheduleResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.response.ScheduleSlotResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.response.ScheduleTimeRowResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entity.Booking;
import io.mpruy.gor_gemilangcondet.backend_api.entity.Court;
import io.mpruy.gor_gemilangcondet.backend_api.enums.BookingStatus;
import io.mpruy.gor_gemilangcondet.backend_api.event.BookingStatusChangedEvent;
import io.mpruy.gor_gemilangcondet.backend_api.repository.BookingRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.CourtRepository;
import io.mpruy.gor_gemilangcondet.backend_api.security.JwtRoleExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Core service untuk fitur Jadwal Real-Time GOR Gemilang Condet.
 *
 * <p>Bertanggung jawab atas:
 * <ol>
 *   <li>Membangun grid jadwal (waktu × lapangan) dari data booking di database.</li>
 *   <li>Menyimpan perubahan status booking dan menerbitkan event agar
 *       WebSocket broadcast dijalankan.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    /** Jam buka GOR (inklusif) */
    static final LocalTime OPEN_TIME  = LocalTime.of(7, 0);
    /** Jam tutup GOR (inklusif, slot 22:00 = malam) */
    static final LocalTime CLOSE_TIME = LocalTime.of(22, 0);

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** Status booking yang dianggap OCCUPIED (slot tidak tersedia) */
    private static final List<BookingStatus> ACTIVE_STATUSES =
            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    private final CourtRepository           courtRepository;
    private final BookingRepository         bookingRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ReservasiClient           reservasiClient;
    private final JwtRoleExtractor          jwtRoleExtractor;

    // ──────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Kembalikan jadwal lengkap untuk {@code date}.
     * Grid dimulai dari jam {@value #OPEN_TIME} sampai {@value #CLOSE_TIME},
     * satu baris per jam, enam kolom per lapangan.
     */
    public ScheduleResponse getSchedule(LocalDate date) {
        return getSchedule(date, false);
    }

    /**
     * Versi internal yang menerima flag {@code showBookerName} untuk mengontrol
     * apakah nama pemesan ditampilkan dalam respons.
     */
    public ScheduleResponse getSchedule(LocalDate date, boolean showBookerName) {

        List<Court>   courts   = courtRepository.findAll();
        List<Booking> bookings = bookingRepository.findActiveByDate(date, ACTIVE_STATUSES);

        // Buat lookup cepat: (courtId, startTime) → Booking
        Map<String, Booking> bookingMap = buildBookingMap(bookings);

        List<ScheduleTimeRowResponse> timeRows = new ArrayList<>();

        for (LocalTime t = OPEN_TIME; !t.isAfter(CLOSE_TIME); t = t.plusHours(1)) {
            String timeLabel = t.format(TIME_FMT);
            List<ScheduleSlotResponse> slots = new ArrayList<>();

            for (Court court : courts) {
                String key    = slotKey(court.getId(), t);
                Booking found = bookingMap.get(key);

                slots.add(buildSlot(court, found, showBookerName));
            }

            timeRows.add(ScheduleTimeRowResponse.builder()
                    .time(timeLabel)
                    .slots(slots)
                    .build());
        }

        return ScheduleResponse.builder()
                .date(date)
                .lastUpdated(LocalDateTime.now())
                .timeSlots(timeRows)
                .build();
    }

    /**
     * Kembalikan jadwal dari backend Fadhil dengan visibility {@code namaWakil}
     * yang bergantung pada role pemanggil.
     *
     * <ul>
     *   <li>ADMIN / OWNER / STAF_LAPANGAN → {@code bookerName} berisi {@code namaWakil}</li>
     *   <li>Role lain / anonim → {@code bookerName} null (disembunyikan)</li>
     * </ul>
     *
     * @param date tanggal jadwal
     * @param role role dari JWT (bisa null untuk pengguna anonim)
     */
    public ScheduleResponse getScheduleFromFadhil(LocalDate date, String role) {

        boolean showBookerName = jwtRoleExtractor.canViewBookerName(role);

        // 1. Ambil ketersediaan per lapangan dari Fadhil
        List<FadhilCourtAvailabilityDto> courts = reservasiClient.getAvailability(date);

        // Fallback: jika Fadhil tidak tersedia, gunakan data lokal (H2 DB)
        if (courts == null || courts.isEmpty()) {
            return getSchedule(date, showBookerName);
        }

        // 2. Jika admin, ambil daftar reservasi untuk lookup namaWakil
        //    Key: "<lapanganId>_<startHour>"  →  namaWakil
        Map<String, String> bookerMap = new HashMap<>();
        if (showBookerName) {
            List<FadhilReservasiDto> reservations = reservasiClient.getAllReservations();
            for (FadhilReservasiDto r : reservations) {
                if (r.getReservationStart() == null || r.getLapanganId() == null) continue;
                // Iterasi setiap jam yang dicakup reservasi
                int startHour = r.getReservationStart().getHour();
                int endHour   = (r.getReservationEnd() != null)
                        ? r.getReservationEnd().getHour() : startHour + 1;
                for (int h = startHour; h < endHour; h++) {
                    String key = r.getLapanganId().toString() + "_" + h;
                    bookerMap.put(key, r.getNamaWakil());
                }
            }
        }

        // 3. Bangun grid jadwal berdasarkan data Fadhil
        //    Susun slot per-jam, semua lapangan dalam satu baris waktu
        //    Kumpulkan jam dari slot yang tersedia/terisi
        Map<Integer, List<ScheduleSlotResponse>> rowMap = new HashMap<>();

        for (FadhilCourtAvailabilityDto court : courts) {
            if (court.getSlots() == null) continue;
            for (FadhilSlotDto slot : court.getSlots()) {
                int hour = slot.getStartHour();
                rowMap.computeIfAbsent(hour, k -> new ArrayList<>());

                String lapanganIdStr = court.getLapanganId() != null
                        ? court.getLapanganId().toString() : null;

                if (slot.isAvailable()) {
                    rowMap.get(hour).add(ScheduleSlotResponse.builder()
                            .lapanganId(lapanganIdStr)
                            .courtName(court.getLapanganName())
                            .status("AVAILABLE")
                            .label("Book Now")
                            .build());
                } else {
                    String bookerName = showBookerName
                            ? bookerMap.get(lapanganIdStr + "_" + hour) : null;
                    rowMap.get(hour).add(ScheduleSlotResponse.builder()
                            .lapanganId(lapanganIdStr)
                            .courtName(court.getLapanganName())
                            .status("BOOKED")
                            .label("Booked")
                            .bookerName(bookerName)
                            .build());
                }
            }
        }

        // 4. Urutkan baris berdasarkan jam
        List<ScheduleTimeRowResponse> timeRows = rowMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> ScheduleTimeRowResponse.builder()
                        .time(String.format("%02d:00", e.getKey()))
                        .slots(e.getValue())
                        .build())
                .toList();

        return ScheduleResponse.builder()
                .date(date)
                .lastUpdated(LocalDateTime.now())
                .timeSlots(timeRows)
                .build();
    }

    /**
     * Dipanggil oleh service lain (misalnya BookingService) setelah
     * menyimpan / mengubah status booking agar broadcast WebSocket dikirim.
     *
     * @param booking booking yang baru saja berubah statusnya
     */
    public void notifySlotChanged(Booking booking) {
        eventPublisher.publishEvent(new BookingStatusChangedEvent(this, booking));
    }

    /**
     * Buat {@link ScheduleUpdateMessage} dari sebuah booking — digunakan oleh
     * {@link io.mpruy.gor_gemilangcondet.backend_api.event.ScheduleBroadcastListener}.
     */
    public ScheduleUpdateMessage buildUpdateMessage(Booking booking) {
        boolean occupied = ACTIVE_STATUSES.contains(booking.getStatus());

        String bookerName = occupied ? booking.getCustomerName() : null;

        return ScheduleUpdateMessage.builder()
                .date(booking.getBookingDate().format(DATE_FMT))
                .courtId(booking.getCourt().getId())
                .courtName(booking.getCourt().getName())
                .time(booking.getStartTime().format(TIME_FMT))
                .status(occupied ? "BOOKED" : "AVAILABLE")
                .label(occupied ? bookerName : "Book Now")
                .bookerName(bookerName)
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    /**
     * Buat booking baru (digunakan oleh endpoint test simulasi).
     * Langsung berstatus CONFIRMED dan memicu broadcast WebSocket.
     *
     * @throws IllegalArgumentException jika lapangan tidak ditemukan atau slot sudah terisi
     */
    @Transactional
    public Booking createTestBooking(Integer courtId, LocalDate date, LocalTime time, String customerName) {
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new IllegalArgumentException("Lapangan tidak ditemukan: " + courtId));

        // Cek apakah slot sudah terisi
        boolean alreadyBooked = bookingRepository.findActiveByDate(date, ACTIVE_STATUSES)
                .stream()
                .anyMatch(b -> b.getCourt().getId().equals(courtId) && b.getStartTime().equals(time));

        if (alreadyBooked) {
            throw new IllegalStateException("Slot sudah dipesan untuk lapangan " + courtId + " jam " + time);
        }

        Booking booking = Booking.builder()
                .court(court)
                .bookingDate(date)
                .startTime(time)
                .customerName(customerName)
                .customerPhone("-")
                .status(BookingStatus.CONFIRMED)
                .build();

        booking = bookingRepository.save(booking);
        notifySlotChanged(booking);
        return booking;
    }

    /**
     * Batalkan booking (status → CANCELLED) dan broadcast perubahan ke WebSocket.
     */
    @Transactional
    public void cancelTestBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking tidak ditemukan: " + bookingId));
        booking.setStatus(BookingStatus.CANCELLED);
        booking = bookingRepository.save(booking);
        notifySlotChanged(booking);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ──────────────────────────────────────────────────────────────────────────

    private Map<String, Booking> buildBookingMap(List<Booking> bookings) {
        Map<String, Booking> map = new HashMap<>();
        for (Booking b : bookings) {
            map.put(slotKey(b.getCourt().getId(), b.getStartTime()), b);
        }
        return map;
    }

    private String slotKey(int courtId, LocalTime time) {
        return courtId + "_" + time.format(TIME_FMT);
    }

    private ScheduleSlotResponse buildSlot(Court court, Booking booking, boolean showBookerName) {
        if (booking == null) {
            return ScheduleSlotResponse.builder()
                    .courtId(court.getId())
                    .courtName(court.getName())
                    .status("AVAILABLE")
                    .label("Book Now")
                    .build();
        }
        String bookerName = showBookerName ? booking.getCustomerName() : null;
        return ScheduleSlotResponse.builder()
                .courtId(court.getId())
                .courtName(court.getName())
                .status("BOOKED")
                .label(showBookerName ? booking.getCustomerName() : "Booked")
                .bookerName(bookerName)
                .bookingId(booking.getId())
                .build();
    }
}
