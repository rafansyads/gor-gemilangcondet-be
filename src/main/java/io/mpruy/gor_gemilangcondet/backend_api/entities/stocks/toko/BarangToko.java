package io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.toko;

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
@Table(name = "barang_toko")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BarangToko extends Barang {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BarangTokoType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BarangTokoStatus status;
}
