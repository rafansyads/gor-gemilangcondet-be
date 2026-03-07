package io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses;

import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.LapanganType;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.ReservasiStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ReservasiResponse {

    private UUID id;
    private UUID userId;
    private UUID lapanganId;
    private String lapanganName;
    private LapanganType lapanganType;
    private LocalDateTime reservationStart;
    private LocalDateTime reservationEnd;
    private int durationInHours;
    private String namaWakil;
    private String nomorTelepon;
    private int jumlahOrang;
    private double totalPayment;
    private ReservasiStatus status;
    private String paymentProofUrl;
    private LocalDateTime paymentDeadline;
    private List<RentItemResponse> rentItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
