package io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.requests;

import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.LapanganType;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateLapanganRequest {

    @NotBlank(message = "Nama lapangan wajib diisi")
    private String name;

    @NotNull(message = "Tipe lapangan wajib diisi")
    private LapanganType type;

    @Min(value = 1, message = "Tarif per jam minimal Rp 1")
    private double tarifPerJam;
}
