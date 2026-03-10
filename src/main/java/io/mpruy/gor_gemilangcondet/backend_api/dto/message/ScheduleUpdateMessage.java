package io.mpruy.gor_gemilangcondet.backend_api.dto.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Pesan WebSocket yang di-broadcast ke klien yang berlangganan
 * topik {@code /topic/schedule/{date}} setiap kali ada perubahan booking.
 *
 * <p>FE cukup mendengarkan topik ini dan memperbarui sel yang sesuai
 * tanpa harus me-refresh seluruh halaman.
 *
 * <pre>
 * {
 *   "date"        : "2024-12-25",
 *   "courtId"     : 3,
 *   "courtName"   : "Court 3",
 *   "time"        : "14:00",
 *   "status"      : "BOOKED",
 *   "bookerName"  : "Budi Santoso",
 *   "lastUpdated" : "25-12-2024 14:05:00"
 * }
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScheduleUpdateMessage {

    /** Format "yyyy-MM-dd" */
    private String date;

    private Integer courtId;
    private String  courtName;

    /** Format "HH:mm" */
    private String time;

    /** "AVAILABLE" atau "BOOKED" */
    private String status;

    /**
     * Label sel jadwal setelah perubahan:
     * "Book Now" jika AVAILABLE, nama pemesan jika BOOKED.
     */
    private String label;

    /** Null jika status = AVAILABLE */
    private String bookerName;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime lastUpdated;
}
