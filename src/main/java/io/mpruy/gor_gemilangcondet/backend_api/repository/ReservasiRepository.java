package io.mpruy.gor_gemilangcondet.backend_api.repository;

import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.Reservasi;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.ReservasiStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReservasiRepository extends JpaRepository<Reservasi, UUID> {

    @Query("SELECT r FROM Reservasi r JOIN FETCH r.lapangan WHERE r.id = :id")
    Optional<Reservasi> findByIdWithLapangan(@Param("id") UUID id);

    @Query("SELECT r FROM Reservasi r JOIN FETCH r.lapangan")
    List<Reservasi> findAllWithLapangan();

    /**
     * Finds active reservations that overlap with the given time range for a specific court.
     * Two intervals [A_start, A_end) and [B_start, B_end) overlap iff A_start < B_end AND A_end > B_start.
     */
    @Query("SELECT r FROM Reservasi r WHERE r.lapangan.id = :lapanganId " +
           "AND r.status NOT IN :excludedStatuses " +
           "AND r.reservationStart < :end AND r.reservationEnd > :start")
    List<Reservasi> findOverlappingReservations(
            @Param("lapanganId") UUID lapanganId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("excludedStatuses") List<ReservasiStatus> excludedStatuses);

    /**
     * Finds all active reservations across all courts that overlap with the given time range.
     * Used for equipment availability checking.
     */
    @Query("SELECT r FROM Reservasi r WHERE r.status NOT IN :excludedStatuses " +
           "AND r.reservationStart < :end AND r.reservationEnd > :start")
    List<Reservasi> findActiveReservationsDuringPeriod(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("excludedStatuses") List<ReservasiStatus> excludedStatuses);

    @Query("SELECT r FROM Reservasi r JOIN FETCH r.lapangan WHERE r.userId = :userId")
    List<Reservasi> findByUserIdWithLapangan(@Param("userId") UUID userId);
}
