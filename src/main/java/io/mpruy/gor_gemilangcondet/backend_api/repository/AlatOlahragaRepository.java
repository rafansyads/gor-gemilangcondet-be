package io.mpruy.gor_gemilangcondet.backend_api.repository;

import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.AlatOlahraga;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.AlatOlahragaStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.BarangType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlatOlahragaRepository extends JpaRepository<AlatOlahraga, UUID> {

    List<AlatOlahraga> findByTypeIn(List<BarangType> types);

    List<AlatOlahraga> findByTypeInAndStatus(List<BarangType> types, AlatOlahragaStatus status);
}
