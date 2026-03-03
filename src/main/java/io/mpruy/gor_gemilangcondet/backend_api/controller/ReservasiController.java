package io.mpruy.gor_gemilangcondet.backend_api.controller;

import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseRequestDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseResponseDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.requests.CreateReservasiRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.requests.RescheduleReservasiRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.*;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.LapanganType;
import io.mpruy.gor_gemilangcondet.backend_api.service.ReservasiService;
import io.mpruy.gor_gemilangcondet.backend_api.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class ReservasiController {

    private final ReservasiService reservasiService;

    // ──────────────────────────────────────────────────────────────────────────
    // GET /bookings — Get all reservations
    // ──────────────────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<BaseResponseDto<List<ReservasiResponse>>> getAllBookings() {
        try {
            List<ReservasiResponse> bookings = reservasiService.getAllReservations();
            return ResponseUtil.success(bookings, "Data reservasi berhasil diambil", HttpStatus.OK)
                    .toBuilder().build();
        } catch (Exception ex) {
            return ResponseUtil.error(
                    "Gagal mengambil data reservasi: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /bookings/{id} — Get reservation by ID
    // ──────────────────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponseDto<ReservasiResponse>> getBookingById(@PathVariable UUID id) {
        try {
            ReservasiResponse booking = reservasiService.getReservationById(id);
            return ResponseUtil.success(booking, "Data reservasi berhasil diambil", HttpStatus.OK)
                    .toBuilder().build();
        } catch (IllegalArgumentException ex) {
            return ResponseUtil.error(ex.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            return ResponseUtil.error(
                    "Gagal mengambil data reservasi: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /bookings/user/{userId} — Get reservations by user
    // ──────────────────────────────────────────────────────────────────────────

    @GetMapping("/user/{userId}")
    public ResponseEntity<BaseResponseDto<List<ReservasiResponse>>> getBookingsByUserId(
            @PathVariable UUID userId) {
        try {
            List<ReservasiResponse> bookings = reservasiService.getReservationsByUserId(userId);
            return ResponseUtil.success(bookings, "Data reservasi user berhasil diambil", HttpStatus.OK)
                    .toBuilder().build();
        } catch (Exception ex) {
            return ResponseUtil.error(
                    "Gagal mengambil data reservasi user: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /bookings/availability — Court availability for a date
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns per-court, per-hour-slot availability.
     *
     * @param date the date to check (ISO format: yyyy-MM-dd)
     * @param type optional court type filter (BADMINTON, FUTSAL, BASKET, VOLI, TENIS)
     */
    @GetMapping("/availability")
    public ResponseEntity<BaseResponseDto<List<CourtAvailabilityResponse>>> getAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) LapanganType type) {
        try {
            List<CourtAvailabilityResponse> availability = reservasiService.getAvailability(date, type);
            return ResponseUtil.success(availability,
                    "Data ketersediaan lapangan berhasil diambil", HttpStatus.OK)
                    .toBuilder().build();
        } catch (Exception ex) {
            return ResponseUtil.error(
                    "Gagal mengambil data ketersediaan: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /bookings/equipment-availability — Equipment availability for a time range
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns available sports equipment filtered by court type for a given time range.
     *
     * @param courtType type of court (determines compatible equipment)
     * @param start     start of the time range (ISO date-time)
     * @param end       end of the time range (ISO date-time)
     */
    @GetMapping("/equipment-availability")
    public ResponseEntity<BaseResponseDto<List<AlatOlahragaAvailabilityResponse>>> getEquipmentAvailability(
            @RequestParam LapanganType courtType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        try {
            List<AlatOlahragaAvailabilityResponse> availability =
                    reservasiService.getEquipmentAvailability(courtType, start, end);
            return ResponseUtil.success(availability,
                    "Data ketersediaan alat olahraga berhasil diambil", HttpStatus.OK)
                    .toBuilder().build();
        } catch (Exception ex) {
            return ResponseUtil.error(
                    "Gagal mengambil data ketersediaan alat: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /bookings/reserve — Create new reservation
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new court reservation with anti-conflict (race condition) validation.
     * Automatically calculates total cost based on court hourly rate and equipment rental.
     */
    @PostMapping("/reserve")
    public ResponseEntity<BaseResponseDto<ReservasiResponse>> createReservation(
            @Validated @RequestBody BaseRequestDto<CreateReservasiRequest> request) {
        try {
            ReservasiResponse response = reservasiService.createReservation(request.getData());
            return ResponseUtil.success(response, "Reservasi berhasil dibuat", HttpStatus.CREATED)
                    .toBuilder().build();
        } catch (IllegalArgumentException ex) {
            return ResponseUtil.error(ex.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception ex) {
            return ResponseUtil.error(
                    "Gagal membuat reservasi: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PUT /bookings/reschedule/{id} — Reschedule existing reservation
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Reschedules an existing reservation to a new time slot.
     * Applies the same anti-conflict validation as creation.
     */
    @PutMapping("/reschedule/{id}")
    public ResponseEntity<BaseResponseDto<ReservasiResponse>> rescheduleReservation(
            @PathVariable UUID id,
            @Validated @RequestBody BaseRequestDto<RescheduleReservasiRequest> request) {
        try {
            ReservasiResponse response = reservasiService.rescheduleReservation(id, request.getData());
            return ResponseUtil.success(response, "Reservasi berhasil dijadwal ulang", HttpStatus.OK)
                    .toBuilder().build();
        } catch (IllegalArgumentException ex) {
            return ResponseUtil.error(ex.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception ex) {
            return ResponseUtil.error(
                    "Gagal menjadwal ulang reservasi: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
