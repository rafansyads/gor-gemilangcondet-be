package io.mpruy.gor_gemilangcondet.backend_api.controller;

import io.mpruy.gor_gemilangcondet.backend_api.entity.Booking;
import io.mpruy.gor_gemilangcondet.backend_api.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

/**
 * Endpoint khusus untuk simulasi / pengujian sistem penjadwalan real-time.
 *
 * <p><strong>Hanya untuk development/testing</strong> — nonaktifkan sebelum production.
 *
 * <h3>Endpoints</h3>
 * <pre>
 * POST   /api/test/book        — Buat booking baru, langsung CONFIRMED
 * DELETE /api/test/book/{id}   — Batalkan booking (status → CANCELLED)
 * </pre>
 */
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*")
public class TestBookingController {

    private final ScheduleService scheduleService;

    /**
     * Buat booking baru untuk slot tertentu.
     *
     * <p>Request body (JSON):
     * <pre>
     * {
     *   "courtId"      : 3,
     *   "date"         : "2026-03-04",
     *   "time"         : "14:00",
     *   "customerName" : "John Doe"
     * }
     * </pre>
     */
    @PostMapping("/book")
    public ResponseEntity<?> createBooking(@RequestBody BookingRequest req) {
        try {
            LocalDate date = LocalDate.parse(req.date());
            LocalTime time = LocalTime.parse(req.time());

            Booking booking = scheduleService.createTestBooking(
                    req.courtId(), date, time, req.customerName()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "id",           booking.getId().toString(),
                    "courtId",      booking.getCourt().getId(),
                    "courtName",    booking.getCourt().getName(),
                    "date",         booking.getBookingDate().toString(),
                    "time",         booking.getStartTime().toString(),
                    "customerName", booking.getCustomerName(),
                    "status",       booking.getStatus().name(),
                    "message",      "Booking berhasil dibuat dan broadcast ke WebSocket"
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Batalkan booking berdasarkan ID.
     * Status booking diubah menjadi CANCELLED dan perubahan di-broadcast ke WebSocket.
     */
    @DeleteMapping("/book/{id}")
    public ResponseEntity<?> cancelBooking(@PathVariable UUID id) {
        try {
            scheduleService.cancelTestBooking(id);
            return ResponseEntity.ok(Map.of(
                    "id",      id.toString(),
                    "message", "Booking dibatalkan dan broadcast ke WebSocket"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /** DTO request untuk membuat booking. */
    public record BookingRequest(
            Integer courtId,
            String  date,
            String  time,
            String  customerName
    ) {}
}
