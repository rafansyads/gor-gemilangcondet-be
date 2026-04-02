package io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InvoiceSlotItem {
    private String reservationId;
    private String courtName;
    private LocalDateTime start;
    private LocalDateTime end;
    private double price;
}
