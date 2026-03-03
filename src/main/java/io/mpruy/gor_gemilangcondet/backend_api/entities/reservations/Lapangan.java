package io.mpruy.gor_gemilangcondet.backend_api.entities.reservations;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lapangan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lapangan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime maintenanceStart;

    private LocalDateTime maintenanceEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LapanganType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LapanganStatus status;
}
