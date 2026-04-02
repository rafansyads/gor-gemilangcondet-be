package io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreateBatchReservasiRequest {

    @NotEmpty(message = "Daftar reservasi tidak boleh kosong")
    @Valid
    private List<CreateReservasiRequest> reservations;
}
