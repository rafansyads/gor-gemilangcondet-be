package io.mpruy.gor_gemilangcondet.backend_api.repository;

import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.Barang;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BarangRepository extends JpaRepository<Barang, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Barang b WHERE b.id = :id")
    Optional<Barang> findByIdWithPessimisticLock(@Param("id") UUID id);

    @Query("SELECT b FROM Barang b WHERE TYPE(b) = Barang ORDER BY b.name")
    List<Barang> findAllSellable();
}
