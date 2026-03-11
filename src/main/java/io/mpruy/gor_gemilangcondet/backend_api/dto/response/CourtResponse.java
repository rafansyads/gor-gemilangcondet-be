package io.mpruy.gor_gemilangcondet.backend_api.dto.response;

import io.mpruy.gor_gemilangcondet.backend_api.entity.Court;
import io.mpruy.gor_gemilangcondet.backend_api.enums.CourtStatus;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourtResponse {

    private Integer id;
    private String name;
    private String code;
    private BigDecimal tarifPerJam;
    private String jenisLantai;
    private String fasilitas;
    private CourtStatus status;

    public static CourtResponse from(Court court) {
        return CourtResponse.builder()
                .id(court.getId())
                .name(court.getName())
                .code(court.getCode())
                .tarifPerJam(court.getTarifPerJam())
                .jenisLantai(court.getJenisLantai())
                .fasilitas(court.getFasilitas())
                .status(court.getStatus())
                .build();
    }
}
