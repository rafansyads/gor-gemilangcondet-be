package io.mpruy.gor_gemilangcondet.backend_api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Respons lengkap endpoint {@code GET /api/schedule?date=yyyy-MM-dd}.
 *
 * <pre>
 * {
 *   "date"        : "2024-12-25",
 *   "lastUpdated" : "2024-12-25 14:30:00",
 *   "timeSlots"   : [
 *     {
 *       "time"  : "07:00",
 *       "slots" : [ ... ]
 *     },
 *     ...
 *   ]
 * }
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleResponse {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    /** Waktu server saat data ini diambil — untuk indikator "Last updated" di FE */
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime lastUpdated;

    /** Baris 07:00 s/d 22:00 (16 baris) */
    private List<ScheduleTimeRowResponse> timeSlots;
}
