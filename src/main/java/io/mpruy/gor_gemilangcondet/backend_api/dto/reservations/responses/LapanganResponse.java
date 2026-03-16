package io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses;

import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.LapanganStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.LapanganType;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class LapanganResponse {

    private UUID id;
    private String name;
    private LapanganType type;
    private LapanganStatus status;
    private double tarifPerJam;
}
