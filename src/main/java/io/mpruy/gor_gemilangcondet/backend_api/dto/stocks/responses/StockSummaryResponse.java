package io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StockSummaryResponse {
    private long totalProduk;
    private long stokRendah;
    private double nilaiStok;
}
