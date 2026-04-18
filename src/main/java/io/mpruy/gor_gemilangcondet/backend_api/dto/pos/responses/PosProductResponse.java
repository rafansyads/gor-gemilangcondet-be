package io.mpruy.gor_gemilangcondet.backend_api.dto.pos.responses;

import io.mpruy.gor_gemilangcondet.backend_api.entities.pos.PosProductCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosProductResponse {
    private UUID id;
    private String name;
    private String sku;
    private double price;
    private long stock;
    private PosProductCategory category;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
