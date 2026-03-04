package io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses;

import io.mpruy.gor_gemilangcondet.backend_api.entities.payment.PaymentMethod;
import io.mpruy.gor_gemilangcondet.backend_api.entities.payment.PaymentStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.payment.PaymentType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PembayaranResponse {

    private UUID id;
    private UUID userId;
    private UUID staffId;
    private UUID reservationId;
    private PaymentMethod method;
    private PaymentType type;
    private PaymentStatus status;
    private double price;
    private String receipt;
    private LocalDateTime paymentDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
