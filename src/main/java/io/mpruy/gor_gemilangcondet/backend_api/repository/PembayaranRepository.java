package io.mpruy.gor_gemilangcondet.backend_api.repository;

import io.mpruy.gor_gemilangcondet.backend_api.entities.payment.Pembayaran;
import io.mpruy.gor_gemilangcondet.backend_api.entities.payment.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PembayaranRepository extends JpaRepository<Pembayaran, UUID> {

    Optional<Pembayaran> findByReservationId(UUID reservationId);

    List<Pembayaran> findByStatus(PaymentStatus status);

    List<Pembayaran> findByUserId(UUID userId);
}
