package io.mpruy.gor_gemilangcondet.backend_api.repository;

import io.mpruy.gor_gemilangcondet.backend_api.entity.Booking;
import io.mpruy.gor_gemilangcondet.backend_api.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    /**
     * Ambil semua booking AKTIF (PENDING atau CONFIRMED) untuk tanggal tertentu.
     * Digunakan oleh ScheduleService untuk membangun grid jadwal.
     */
    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.lapangan
        WHERE b.bookingDate = :date
          AND b.status IN :statuses
        ORDER BY b.lapangan.name ASC, b.startTime ASC
        """)
    List<Booking> findActiveByDate(
            @Param("date") LocalDate date,
            @Param("statuses") List<BookingStatus> statuses
    );
}
