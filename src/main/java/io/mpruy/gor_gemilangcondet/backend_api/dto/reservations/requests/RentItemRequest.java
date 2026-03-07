package io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.requests;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.UUID;

@Data
public class RentItemRequest {

    @NotNull(message = "ID alat olahraga wajib diisi")
    private UUID alatOlahragaId;

    @Min(value = 1, message = "Jumlah minimal 1")
    private int quantity;
}
