package io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.requests;

import io.mpruy.gor_gemilangcondet.backend_api.entities.payment.PaymentMethod;
import io.mpruy.gor_gemilangcondet.backend_api.entities.payment.PaymentType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * Request payload for staff to confirm a reservation payment.
 */
@Data
public class ConfirmPaymentRequest {

    @NotNull(message = "ID reservasi wajib diisi")
    private UUID reservationId;

    @NotNull(message = "Metode pembayaran wajib diisi")
    private PaymentMethod method;

    @NotNull(message = "Tipe pembayaran wajib diisi")
    private PaymentType type;

    /** Optional receipt reference (e.g. transfer proof or QRIS code). */
    private String receipt;
}
