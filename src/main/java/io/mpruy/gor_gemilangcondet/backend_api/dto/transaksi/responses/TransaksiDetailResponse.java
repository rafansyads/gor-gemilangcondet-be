package io.mpruy.gor_gemilangcondet.backend_api.dto.transaksi.responses;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TransaksiDetailResponse {
    private UUID id;
    private UUID barangId;
    private String barangName;
    private double hargaSatuan;
    private int kuantitas;
    private double subtotal;
}
