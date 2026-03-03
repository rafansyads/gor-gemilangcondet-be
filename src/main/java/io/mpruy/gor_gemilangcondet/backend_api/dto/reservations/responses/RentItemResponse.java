package io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class RentItemResponse {

    private UUID alatOlahragaId;
    private String name;
    private int quantity;
    private double pricePerUnit;
    private double subtotal;
}
