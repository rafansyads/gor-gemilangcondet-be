package io.mpruy.gor_gemilangcondet.backend_api.repository;

import io.mpruy.gor_gemilangcondet.backend_api.entity.Court;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourtRepository extends JpaRepository<Court, Integer> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Integer id);
}
