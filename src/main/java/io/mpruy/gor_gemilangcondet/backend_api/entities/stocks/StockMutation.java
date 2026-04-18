package io.mpruy.gor_gemilangcondet.backend_api.entities.stocks;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_mutation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMutation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barang_id", nullable = false)
    private Barang barang;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockMutationDirection direction;

    @Column(nullable = false)
    private long quantity;

    @Column(nullable = false)
    private long beforeStock;

    @Column(nullable = false)
    private long afterStock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockMutationSource source;

    @Column(length = 255)
    private String reason;

    private UUID actorStaffId;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
