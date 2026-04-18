package io.mpruy.gor_gemilangcondet.backend_api.repository;

import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.alat_olahraga.AlatOlahraga;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.alat_olahraga.AlatOlahragaStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.alat_olahraga.AlatOlahragaType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlatOlahragaRepository extends JpaRepository<AlatOlahraga, UUID> {

    List<AlatOlahraga> findByTypeIn(List<AlatOlahragaType> types);

    List<AlatOlahraga> findByTypeInAndStatus(List<AlatOlahragaType> types, AlatOlahragaStatus status);
}
