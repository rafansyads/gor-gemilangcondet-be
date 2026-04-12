package io.mpruy.gor_gemilangcondet.backend_api.dto.transaksi.responses;

import io.mpruy.gor_gemilangcondet.backend_api.entities.payment.PaymentMethod;
import io.mpruy.gor_gemilangcondet.backend_api.entities.transaksi.TransaksiStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class TransaksiResponse {
    private UUID id;
    private UUID staffId;
    private PaymentMethod paymentMethod;
    private TransaksiStatus status;
    private double subtotal;
    private double diskon;
    private double grandTotal;
    private List<TransaksiDetailResponse> items;
    private LocalDateTime createdAt;
}
