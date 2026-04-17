package io.mpruy.gor_gemilangcondet.backend_api.entities.reservations;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lapangan_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LapanganLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "lapangan_id", nullable = false)
    private UUID lapanganId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String username;

    @Column(name = "change_description", nullable = false, columnDefinition = "TEXT")
    private String changeDescription;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
