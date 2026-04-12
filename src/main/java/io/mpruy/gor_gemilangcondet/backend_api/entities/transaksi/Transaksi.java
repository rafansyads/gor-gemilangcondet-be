package io.mpruy.gor_gemilangcondet.backend_api.entities.transaksi;

import io.mpruy.gor_gemilangcondet.backend_api.entities.payment.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "transaksi")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaksi {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Staf yang memproses transaksi */
    @Column(nullable = false)
    private UUID staffId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransaksiStatus status;

    @Column(nullable = false)
    private double subtotal;

    @Column(nullable = false)
    private double diskon;

    @Column(nullable = false)
    private double grandTotal;

    @OneToMany(mappedBy = "transaksi", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TransaksiDetail> details = new ArrayList<>();

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
