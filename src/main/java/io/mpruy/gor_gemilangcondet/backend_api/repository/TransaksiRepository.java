package io.mpruy.gor_gemilangcondet.backend_api.repository;

import io.mpruy.gor_gemilangcondet.backend_api.entities.transaksi.Transaksi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransaksiRepository extends JpaRepository<Transaksi, UUID> {

    @Query("SELECT t FROM Transaksi t LEFT JOIN FETCH t.details d LEFT JOIN FETCH d.barang WHERE t.id = :id")
    Optional<Transaksi> findByIdWithDetails(@Param("id") UUID id);

    List<Transaksi> findByStaffIdOrderByCreatedAtDesc(UUID staffId);

    @Query("SELECT t FROM Transaksi t LEFT JOIN FETCH t.details d LEFT JOIN FETCH d.barang ORDER BY t.createdAt DESC")
    List<Transaksi> findAllWithDetails();
}
