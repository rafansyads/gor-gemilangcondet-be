package io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses;

import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.StockMutationDirection;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.StockMutationSource;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class StockCardEntryResponse {
    private LocalDateTime waktu;
    private StockMutationDirection arah;
    private StockMutationSource sumber;
    private long jumlah;
    private long stokSebelum;
    private long stokSesudah;
    private String alasan;
    private UUID staffId;
}
