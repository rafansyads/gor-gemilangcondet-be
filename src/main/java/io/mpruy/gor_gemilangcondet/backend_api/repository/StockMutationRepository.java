package io.mpruy.gor_gemilangcondet.backend_api.repository;

import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.StockMutation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StockMutationRepository extends JpaRepository<StockMutation, UUID> {
    List<StockMutation> findByBarangIdOrderByCreatedAtDesc(UUID barangId);
}
