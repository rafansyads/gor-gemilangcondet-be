package io.mpruy.gor_gemilangcondet.backend_api.entities.stocks;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "barang")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Barang {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String name;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private long stock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BarangType type;

    @Column(unique = true, nullable = false)
    private String sku;

    @Column(nullable = false)
    private double purchasePrice;

    @Column(nullable = false)
    private double price;

    private String unit;

    private String imageUrl;
}
