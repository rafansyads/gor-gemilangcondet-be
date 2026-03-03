package io.mpruy.gor_gemilangcondet.backend_api.entities.stocks;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "alat_olahraga")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AlatOlahraga extends Barang {

    private LocalDateTime reservationStart;

    private LocalDateTime reservationEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlatOlahragaStatus status;
}
