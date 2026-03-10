package io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SlotAvailabilityResponse {

    private int startHour;
    private int endHour;
    private boolean available;
}
