package io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses;

import io.mpruy.gor_gemilangcondet.backend_api.entities.payment.PaymentStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.ReservasiStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Response returned after a successful payment confirmation.
 * Includes updated status for both the payment and the reservation.
 */
@Data
@Builder
public class ConfirmPaymentResponse {

    private UUID reservationId;
    private ReservasiStatus reservationStatus;
    private UUID paymentId;
    private PaymentStatus paymentStatus;
    private String lapanganName;
    private String message;
}
