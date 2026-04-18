package io.mpruy.gor_gemilangcondet.backend_api.repository;

import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.toko.BarangToko;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BarangTokoRepository extends JpaRepository<BarangToko, UUID> {
    List<BarangToko> findAllByOrderByNameAsc();
}
