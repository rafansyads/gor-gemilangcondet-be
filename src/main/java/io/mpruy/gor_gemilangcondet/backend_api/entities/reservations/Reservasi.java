package io.mpruy.gor_gemilangcondet.backend_api.entities.reservations;

import jakarta.persistence.*;
import lombok.*;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lapangan_id", nullable = false)
    private Lapangan lapangan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservasiStatus status;

    @ElementCollection
    @CollectionTable(name = "reservasi_rent_list", joinColumns = @JoinColumn(name = "reservasi_id"))
    @Column(name = "barang_id")
    private List<UUID> rentList;
}
