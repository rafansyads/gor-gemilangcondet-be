package io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.requests;

import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.StockMutationDirection;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class StockAdjustmentRequest {

    @NotNull(message = "Barang ID wajib diisi")
    private UUID barangId;

    @NotNull(message = "Arah mutasi wajib diisi")
    private StockMutationDirection direction;

    @Min(value = 1, message = "Kuantitas harus lebih dari 0")
    private long quantity;

    @NotBlank(message = "Alasan penyesuaian wajib diisi")
    private String reason;
}
