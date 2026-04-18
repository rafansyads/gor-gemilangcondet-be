package io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.alat_olahraga;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.Barang;

@Entity
@Table(name = "alat_olahraga")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AlatOlahraga extends Barang {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlatOlahragaType type;

    private LocalDateTime reservationStart;

    private LocalDateTime reservationEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlatOlahragaStatus status;
}
