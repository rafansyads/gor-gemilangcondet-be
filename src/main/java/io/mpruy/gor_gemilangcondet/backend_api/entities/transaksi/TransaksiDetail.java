package io.mpruy.gor_gemilangcondet.backend_api.entities.transaksi;

import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.Barang;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "transaksi_detail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransaksiDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaksi_id", nullable = false)
    private Transaksi transaksi;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "barang_id", nullable = false)
    private Barang barang;

    /** Harga satuan saat transaksi (snapshot, agar tidak berubah jika harga produk diubah) */
    @Column(nullable = false)
    private double hargaSatuan;

    @Column(nullable = false)
    private int kuantitas;

    @Column(nullable = false)
    private double subtotal;
}
