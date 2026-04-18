package io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses;

import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.alat_olahraga.AlatOlahragaType;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AlatOlahragaAvailabilityResponse {

    private UUID id;
    private String name;
    private AlatOlahragaType type;
    private double price;
    private long totalStock;
    private long rented;
    private long available;
}
