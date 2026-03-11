package io.mpruy.gor_gemilangcondet.backend_api.controller;

import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseResponseDto;
import io.mpruy.gor_gemilangcondet.backend_api.entity.Booking;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ConflictException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ResourceNotFoundException;
import io.mpruy.gor_gemilangcondet.backend_api.service.ScheduleService;
import io.mpruy.gor_gemilangcondet.backend_api.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
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
 * <p>
 * <strong>Hanya untuk development/testing</strong> — nonaktifkan sebelum
 * production.
 *
 * <h3>Endpoints</h3>
 *
 * <pre>
 * POST   /api/test/book        — Buat booking baru, langsung CONFIRMED
 * DELETE /api/test/book/{id}   — Batalkan booking (status → CANCELLED)
 * </pre>
 */
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*")
public class TestBookingController {

    private final ScheduleService scheduleService;

    /**
     * Buat booking baru untuk slot tertentu.
     *
     * <p>
     * Request body (JSON):
     *
     * <pre>
     * {
     *   "courtId"      : 3,
     *   "date"         : "2026-03-04",
     *   "time"         : "14:00",
     *   "customerName" : "John Doe"
     * }
     * </pre>
     *
     * @throws ConflictException jika slot sudah dipesan
     */
    @PostMapping("/book")
    public ResponseEntity<BaseResponseDto<Map<String, Object>>> createBooking(@RequestBody BookingRequest req) {
        LocalDate date = LocalDate.parse(req.date());
        LocalTime time = LocalTime.parse(req.time());

        Booking booking;
        try {
            booking = scheduleService.createTestBooking(req.courtId(), date, time, req.customerName());
        } catch (IllegalStateException e) {
            throw new ConflictException(e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }

        Map<String, Object> data = Map.of(
                "id", booking.getId().toString(),
                "courtId", booking.getCourt().getId(),
                "courtName", booking.getCourt().getName(),
                "date", booking.getBookingDate().toString(),
                "time", booking.getStartTime().toString(),
                "customerName", booking.getCustomerName(),
                "status", booking.getStatus().name());
        return ResponseUtil.success(data, "Booking berhasil dibuat dan broadcast ke WebSocket",
                HttpStatus.CREATED).toBuilder().build();
    }

    /**
     * Batalkan booking berdasarkan ID.
     * Status booking diubah menjadi CANCELLED dan perubahan di-broadcast ke
     * WebSocket.
     *
     * @throws ResourceNotFoundException jika booking dengan ID tersebut tidak
     *                                   ditemukan
     */
    @DeleteMapping("/book/{id}")
    public ResponseEntity<BaseResponseDto<Map<String, String>>> cancelBooking(@PathVariable UUID id) {
        try {
            scheduleService.cancelTestBooking(id);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
        return ResponseUtil.success(
                Map.of("id", id.toString()),
                "Booking dibatalkan dan broadcast ke WebSocket",
                HttpStatus.OK).toBuilder().build();
    }

    /** DTO request untuk membuat booking. */
    public record BookingRequest(
            Integer courtId,
            String date,
            String time,
            String customerName) {
    }
}
