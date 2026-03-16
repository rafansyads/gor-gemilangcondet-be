package io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class CreateReservasiRequest {

    @NotNull(message = "ID lapangan wajib diisi")
    private UUID lapanganId;

    @NotNull(message = "Waktu mulai reservasi wajib diisi")
    private LocalDateTime reservationStart;

    @Min(value = 1, message = "Durasi minimal 1 jam")
    @Max(value = 5, message = "Durasi maksimal 5 jam")
    private int durationInHours;

    @NotNull(message = "ID user wajib diisi")
    private UUID userId;

    @NotBlank(message = "Nama perwakilan wajib diisi")
    private String namaWakil;

    @NotBlank(message = "Nomor telepon wajib diisi")
    @Pattern(regexp = "^[0-9]+$", message = "Nomor telepon hanya boleh berisi angka")
    @Size(min = 9, max = 15, message = "Nomor telepon harus antara 9-15 digit")
    private String nomorTelepon;

    @Min(value = 1, message = "Jumlah orang minimal 1")
    private int jumlahOrang;

    @Valid
    private List<RentItemRequest> rentItems;
}
