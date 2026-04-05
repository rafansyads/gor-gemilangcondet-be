package io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class BatchReservasiResponse {

    private UUID batchId;
    private List<ReservasiResponse> reservations;
    private double totalPayment;
    /** ID of the first reservation — use this for payment-proof upload. */
    private UUID primaryReservationId;
}
