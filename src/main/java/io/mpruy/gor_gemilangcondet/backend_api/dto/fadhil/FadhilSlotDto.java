package io.mpruy.gor_gemilangcondet.backend_api.dto.fadhil;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Satu slot waktu dari endpoint GET /bookings/availability Fadhil.
 * Slot selalu berdurasi 1 jam: startHour → endHour.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FadhilSlotDto {

    /** Jam mulai slot, contoh: 9 berarti 09:00 */
    private int startHour;

    /** Jam selesai slot, contoh: 10 berarti 10:00 */
    private int endHour;

    /** true = slot tersedia, false = slot sudah dipesan */
    private boolean available;
}
