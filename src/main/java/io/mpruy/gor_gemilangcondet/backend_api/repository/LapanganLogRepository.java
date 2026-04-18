package io.mpruy.gor_gemilangcondet.backend_api.repository;

import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.LapanganLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LapanganLogRepository extends JpaRepository<LapanganLog, UUID> {
    List<LapanganLog> findByLapanganIdOrderByCreatedAtDesc(UUID lapanganId);
}
