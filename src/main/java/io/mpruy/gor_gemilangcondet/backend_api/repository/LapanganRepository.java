package io.mpruy.gor_gemilangcondet.backend_api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.Lapangan;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.LapanganType;
import jakarta.persistence.LockModeType;

@Repository
public interface LapanganRepository extends JpaRepository<Lapangan, UUID> {

    List<Lapangan> findByType(LapanganType type);

    Optional<Lapangan> findByName(String name);

    boolean existsByKode(String kode);

    /**
     * Acquires a pessimistic write lock on the court row to prevent
     * race conditions during concurrent reservation attempts.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Lapangan l WHERE l.id = :id")
    Optional<Lapangan> findByIdWithPessimisticLock(@Param("id") UUID id);
}
