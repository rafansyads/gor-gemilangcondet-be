package io.mpruy.gor_gemilangcondet.backend_api.entities.payment;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pembayaran")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pembayaran {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    private UUID staffId;

    @Column(nullable = false)
    private UUID reservationId;

    private LocalDateTime updatedAt;

    private LocalDateTime createdAt;

    private LocalDateTime paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private double price;

    private String receipt;
}
