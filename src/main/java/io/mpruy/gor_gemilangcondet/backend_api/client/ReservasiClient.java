package io.mpruy.gor_gemilangcondet.backend_api.client;

import io.mpruy.gor_gemilangcondet.backend_api.dto.fadhil.FadhilBaseResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.fadhil.FadhilCourtAvailabilityDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.fadhil.FadhilReservasiDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * HTTP client untuk memanggil backend Fadhil (Reservasi lapangan).
 *
 * <p>Digunakan oleh {@link io.mpruy.gor_gemilangcondet.backend_api.service.ScheduleService}
 * untuk mendapatkan data ketersediaan lapangan dan daftar reservasi aktif dari
 * backend Fadhil sebagai sumber data primer jadwal real-time.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservasiClient {

    @Qualifier("fadhilRestClient")
    private final RestClient restClient;

    /**
     * Ambil ketersediaan slot per lapangan untuk tanggal tertentu.
     *
     * <p>Memanggil {@code GET /bookings/availability?date={date}}.
     * Setiap entri berisi daftar slot jam-an dengan status {@code available: boolean}.
     *
     * @param date tanggal yang dicek
     * @return daftar ketersediaan per lapangan; list kosong jika gagal
     */
    public List<FadhilCourtAvailabilityDto> getAvailability(LocalDate date) {
        try {
            FadhilBaseResponse<List<FadhilCourtAvailabilityDto>> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/bookings/availability")
                            .queryParam("date", date.toString())
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || response.getData() == null) {
                log.warn("[FADHIL] getAvailability({}) mengembalikan null", date);
                return Collections.emptyList();
            }
            log.debug("[FADHIL] getAvailability({}) → {} lapangan", date, response.getData().size());
            return response.getData();

        } catch (RestClientException ex) {
            log.error("[FADHIL] Gagal getAvailability({}): {}", date, ex.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Ambil seluruh reservasi — digunakan untuk lookup {@code namaWakil}
     * yang hanya ditampilkan kepada admin/staf.
     *
     * <p>Memanggil {@code GET /bookings}.
     *
     * @return daftar semua reservasi; list kosong jika gagal
     */
    public List<FadhilReservasiDto> getAllReservations() {
        try {
            FadhilBaseResponse<List<FadhilReservasiDto>> response = restClient.get()
                    .uri("/bookings")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || response.getData() == null) {
                log.warn("[FADHIL] getAllReservations() mengembalikan null");
                return Collections.emptyList();
            }
            log.debug("[FADHIL] getAllReservations() → {} reservasi", response.getData().size());
            return response.getData();

        } catch (RestClientException ex) {
            log.error("[FADHIL] Gagal getAllReservations(): {}", ex.getMessage());
            return Collections.emptyList();
        }
    }
}
