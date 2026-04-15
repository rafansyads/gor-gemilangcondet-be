package io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.kantin;

import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.Barang;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "barang_kantin")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BarangKantin extends Barang {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BarangKantinType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BarangKantinStatus status;

    @Column(nullable = false)
    private long reorderThreshold;
}
