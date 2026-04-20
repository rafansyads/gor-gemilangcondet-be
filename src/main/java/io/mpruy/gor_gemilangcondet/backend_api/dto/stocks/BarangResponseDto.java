package io.mpruy.gor_gemilangcondet.backend_api.dto.stocks;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarangResponseDto {
    private UUID id;
    private String name;
    private String category;
    private String sku;
    private double purchasePrice;
    private double price;
    private long stock;
    private String unit;
    private String imageUrl;
}
