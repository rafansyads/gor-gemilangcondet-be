package io.mpruy.gor_gemilangcondet.backend_api.entities.reservations;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String kode;

    private String jenisLantai;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "lapangan_fasilitas", joinColumns = @JoinColumn(name = "lapangan_id"))
    @Column(name = "fasilitas")
    @Builder.Default
    private List<String> fasilitas = new ArrayList<>();

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

    @Column(nullable = false)
    private double tarifPerJam;

    /** Relative path to the court image, served at /api/courts/image/{filename} */
    private String imageUrl;
}
