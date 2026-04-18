package io.mpruy.gor_gemilangcondet.backend_api.dto.stocks;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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

    @Min(value = 0, message = "Harga jual tidak boleh negatif")
    private double price;

    @Min(value = 0, message = "Stok awal tidak boleh negatif")
    private long stock;

    private String unit;
}
