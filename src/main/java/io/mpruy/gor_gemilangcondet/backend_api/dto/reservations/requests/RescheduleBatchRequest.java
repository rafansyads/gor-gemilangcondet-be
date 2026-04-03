package io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.requests;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class RescheduleBatchRequest {

    @NotEmpty(message = "Daftar jam baru tidak boleh kosong")
    private List<LocalDateTime> newStarts;

    /** Optional: if provided, all reservations in the batch will be moved to this court. */
    private UUID newLapanganId;
}
