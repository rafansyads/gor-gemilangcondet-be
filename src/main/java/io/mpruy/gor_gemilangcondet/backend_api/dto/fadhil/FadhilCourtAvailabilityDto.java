package io.mpruy.gor_gemilangcondet.backend_api.dto.fadhil;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Ketersediaan slot per lapangan dari endpoint GET /bookings/availability Fadhil.
 * Satu entri = satu lapangan dengan daftar slot jam-an.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FadhilCourtAvailabilityDto {

    private UUID lapanganId;
    private String lapanganName;

    /** Tipe lapangan: BADMINTON */
    private String lapanganType;

    private double tarifPerJam;
    private LocalDate date;

    /** Daftar slot jam dari jam buka hingga jam tutup */
    private List<FadhilSlotDto> slots;
}
