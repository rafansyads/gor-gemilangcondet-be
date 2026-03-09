package io.mpruy.gor_gemilangcondet.backend_api.entities.reservations;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reservasi")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservasi {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID paymentId;

    @Column(nullable = false)
    private double totalPayment;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private LocalDateTime reservationStart;

    @Column(nullable = false)
    private LocalDateTime reservationEnd;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String namaWakil;

    @Column(nullable = false)
    private String nomorTelepon;

    @Column(nullable = false)
    private int jumlahOrang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lapangan_id", nullable = false)
    private Lapangan lapangan;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    private ReservasiStatus status;

    /** Path/URL to uploaded payment proof image. */
    private String paymentProofUrl;

    /** Deadline for payment (createdAt + 10 min). Null if not applicable. */
    private LocalDateTime paymentDeadline;

    @ElementCollection
    @CollectionTable(name = "reservasi_rent_list", joinColumns = @JoinColumn(name = "reservasi_id"))
    @Column(name = "barang_id")
    private List<UUID> rentList;
}
