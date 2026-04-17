package io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class LapanganLogResponse {
    private UUID id;
    private UUID lapanganId;
    private UUID userId;
    private String username;
    private String changeDescription;
    private LocalDateTime createdAt;
}
