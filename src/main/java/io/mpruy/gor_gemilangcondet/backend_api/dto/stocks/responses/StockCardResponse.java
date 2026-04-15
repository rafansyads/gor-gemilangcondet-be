package io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class StockCardResponse {
    private UUID barangId;
    private String namaBarang;
    private List<StockCardEntryResponse> entries;
}
