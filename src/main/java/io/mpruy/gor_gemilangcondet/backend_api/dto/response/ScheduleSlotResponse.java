package io.mpruy.gor_gemilangcondet.backend_api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.UUID;

/**
 * Status satu slot lapangan pada satu jam tertentu.
 * Ditampilkan sebagai sel pada tabel jadwal.
 *
 * <pre>
 * // Slot tersedia:
 * {
 *   "courtId"   : 3,
 *   "courtName" : "Court 3",
 *   "status"    : "AVAILABLE",
 *   "label"     : "Book Now"
 * }
 *
 * // Slot sudah dipesan:
 * {
 *   "courtId"   : 3,
 *   "courtName" : "Court 3",
 *   "status"    : "BOOKED",
 *   "label"     : "Budi Santoso",
 *   "bookerName": "Budi Santoso"
 * }
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScheduleSlotResponse {

    private Integer courtId;
    private String  courtName;

    /** "AVAILABLE" atau "BOOKED" */
    private String status;

    /**
     * Label yang ditampilkan di sel tabel jadwal.
     * <ul>
     *   <li>Slot AVAILABLE → "Book Now"</li>
     *   <li>Slot BOOKED    → nama pemesan (mis. "Budi Santoso")</li>
     * </ul>
     */
    private String label;

    /**
     * Nama pemesan — hanya terisi jika status BOOKED.
     * Field terpisah dari {@code label} agar FE bisa menggunakannya
     * untuk keperluan lain (tooltip, modal detail, dll.).
     */
    private String bookerName;

    /**
     * UUID booking yang menempati slot ini — hanya terisi jika status BOOKED.
     * Digunakan FE untuk memanggil endpoint cancel.
     */
    private UUID bookingId;

    /**
     * UUID lapangan dari sistem Fadhil — hanya terisi ketika data berasal dari
     * backend Fadhil ({@code getScheduleFromFadhil}). Null ketika berasal dari DB lokal.
     */
    private String lapanganId;
}
