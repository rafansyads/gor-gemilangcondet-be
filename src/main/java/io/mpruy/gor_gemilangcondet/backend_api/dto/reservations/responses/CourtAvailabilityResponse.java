package io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses;

import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.LapanganStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.LapanganType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CourtAvailabilityResponse {

    private UUID lapanganId;
    private String lapanganName;
    private LapanganType lapanganType;
    private LapanganStatus status;
    private double tarifPerJam;
    private LocalDate date;
    private String imageUrl;
    private List<SlotAvailabilityResponse> slots;
}
