package io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.requests;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class RescheduleReservasiRequest {

    @NotNull(message = "Waktu mulai reservasi baru wajib diisi")
    private LocalDateTime newReservationStart;

    @Min(value = 1, message = "Durasi minimal 1 jam")
    @Max(value = 5, message = "Durasi maksimal 5 jam")
    private int durationInHours;

    /** Optional: if provided, the reservation will be moved to this court. */
    private UUID newLapanganId;
}
