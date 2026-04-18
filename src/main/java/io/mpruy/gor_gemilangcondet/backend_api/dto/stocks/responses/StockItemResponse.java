package io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class StockItemResponse {
    private UUID barangId;
    private String kode;
    private String nama;
    private String kategori;
    private String itemType;
    private double harga;
    private long stok;
    private long ambangBatas;
    private String status;
    private double nilaiStok;
}
