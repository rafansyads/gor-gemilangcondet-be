package io.mpruy.gor_gemilangcondet.backend_api.dto.stocks;

import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.BarangType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarangRequestDto {

    @NotBlank(message = "Nama produk tidak boleh kosong")
    private String name;

    @NotBlank(message = "SKU tidak boleh kosong")
    private String sku;

    @NotNull(message = "Kategori tidak boleh kosong")
    private BarangType type;

    @Min(value = 0, message = "Harga beli tidak boleh negatif")
    private double purchasePrice;

    @Min(value = 0, message = "Harga jual tidak boleh negatif")
    private double price;

    @Min(value = 0, message = "Stok awal tidak boleh negatif")
    private long stock;

    private String unit;
}
