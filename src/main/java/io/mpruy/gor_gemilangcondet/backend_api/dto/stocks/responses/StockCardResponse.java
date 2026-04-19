package io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class StockCardResponse {
    private UUID barangId;
    private String kode;
    private String namaBarang;
    private String kategori;
    private String itemType;
    private double harga;
    private long stok;
    private long ambangBatas;
    private String status;
    private double nilaiStok;
    private String unit;
    private String imageUrl;
    private List<StockCardEntryResponse> entries;
}
