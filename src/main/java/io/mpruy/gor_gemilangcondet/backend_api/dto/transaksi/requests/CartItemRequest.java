package io.mpruy.gor_gemilangcondet.backend_api.dto.transaksi.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CartItemRequest {

    @NotNull(message = "ID barang tidak boleh kosong")
    private UUID barangId;

    @Min(value = 1, message = "Kuantitas minimal 1")
    private int kuantitas;
}
