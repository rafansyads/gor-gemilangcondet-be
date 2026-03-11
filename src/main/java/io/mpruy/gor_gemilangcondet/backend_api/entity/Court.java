package io.mpruy.gor_gemilangcondet.backend_api.entity;

import io.mpruy.gor_gemilangcondet.backend_api.enums.CourtStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Representasi satu lapangan di GOR Gemilang Condet.
 */
@Entity
@Table(name = "courts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Court {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Nama tampilan, mis. "Lapangan 1" */
    @Column(nullable = false, length = 50)
    @NotBlank
    private String name;

    /** Kode unik lapangan, mis. "CRT-001" */
    @Column(nullable = false, unique = true, length = 20)
    @NotBlank
    @Pattern(regexp = "CRT-\\d{3}", message = "Format kode harus CRT-XXX (misal: CRT-001)")
    private String code;

    /** Tarif per jam dalam rupiah - tidak boleh negatif */
    @Column(nullable = false, precision = 12, scale = 2)
    @NotNull
    @DecimalMin(value = "0.0", inclusive = true, message = "Tarif tidak boleh negatif")
    private BigDecimal tarifPerJam;

    /** Jenis lantai, mis. "Kayu", "Vinyl", "Semen" */
    @Column(nullable = false, length = 50)
    @NotBlank
    private String jenisLantai;

    /** Deskripsi fasilitas, mis. "LED, Fan, AC" - tidak boleh kosong */
    @Column(nullable = false, length = 255)
    @NotBlank(message = "Deskripsi fasilitas tidak boleh kosong")
    private String fasilitas;

    /** Status operasional lapangan */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CourtStatus status = CourtStatus.ACTIVE;

    /**
     * ID lapangan di sistem Fadhil (API eksternal), nullable bila belum terhubung.
     * Digunakan untuk mencocokkan data dari Fadhil API dengan lapangan lokal.
     */
    @Column(name = "lapangan_id")
    private UUID lapanganId;
}
