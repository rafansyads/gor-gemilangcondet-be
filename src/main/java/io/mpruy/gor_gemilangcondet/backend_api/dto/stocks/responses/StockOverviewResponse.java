package io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StockOverviewResponse {
    private StockSummaryResponse summary;
    private List<StockItemResponse> items;
}
