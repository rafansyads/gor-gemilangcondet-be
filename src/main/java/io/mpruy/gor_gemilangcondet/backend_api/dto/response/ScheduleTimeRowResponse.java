package io.mpruy.gor_gemilangcondet.backend_api.dto.response;

import lombok.*;

import java.util.List;

/**
 * Satu baris pada tabel jadwal — mewakili 1 jam tertentu.
 *
 * <pre>
 * {
 *   "time" : "07:00",
 *   "slots": [ { courtId:1, status:"AVAILABLE" }, ... ]
 * }
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleTimeRowResponse {

    /** Label waktu, format "HH:mm", mis. "07:00", "08:00", … "22:00" */
    private String time;

    /** 6 slot, satu per lapangan, urut courtId 1–6 */
    private List<ScheduleSlotResponse> slots;
}
