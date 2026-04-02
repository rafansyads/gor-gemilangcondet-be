package io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses;

import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.LapanganStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.LapanganType;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class LapanganResponse {
    private String imageUrl;

    private UUID id;
    private String name;
    private String kode;
    private LapanganType type;
    private LapanganStatus status;
    private String jenisLantai;
    private List<String> fasilitas;
    private double tarifPerJam;
}
