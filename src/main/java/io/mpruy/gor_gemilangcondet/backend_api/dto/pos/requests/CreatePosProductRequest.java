package io.mpruy.gor_gemilangcondet.backend_api.dto.pos.requests;

import io.mpruy.gor_gemilangcondet.backend_api.entities.pos.PosProductCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePosProductRequest {

    @NotBlank(message = "Nama produk wajib diisi")
    private String name;

    @NotBlank(message = "SKU produk wajib diisi")
    private String sku;

    @PositiveOrZero(message = "Harga tidak boleh negatif")
    private double price;

    @PositiveOrZero(message = "Stok tidak boleh negatif")
    private long stock;

    @NotNull(message = "Kategori produk wajib dipilih")
    private PosProductCategory category;

    private String imageUrl;
}
