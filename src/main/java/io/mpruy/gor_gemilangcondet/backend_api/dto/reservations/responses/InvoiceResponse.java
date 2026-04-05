package io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class InvoiceResponse {
    private String invoiceId;           // batchId or reservationId
    private String invoiceNumber;       // INV-YYYYMMDD-XXXXX
    private String representativeName;
    private double totalAmount;
    private int slotCount;
    private String status;              // ReservasiStatus name
    private String primaryReservationId;
    private String paymentProofUrl;
    private LocalDateTime createdAt;
    private List<InvoiceSlotItem> slots;
}
