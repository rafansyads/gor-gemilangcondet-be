package io.mpruy.gor_gemilangcondet.backend_api.entity;

import io.mpruy.gor_gemilangcondet.backend_api.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Satu record booking lapangan untuk slot 1 jam.
 * <p>
 * Slot dinyatakan BOOKED pada jadwal real-time apabila status = PENDING | CONFIRMED.
 * Status CANCELLED mengembalikan slot ke AVAILABLE.
 */
@Entity
@Table(
    name = "bookings",
    uniqueConstraints = {
        // Satu slot (lapangan + tanggal + jam mulai) hanya boleh dipesan satu kali aktif.
        @UniqueConstraint(
            name = "uq_booking_court_date_time",
            columnNames = {"court_id", "booking_date", "start_time"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Lapangan yang dipesan */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;

    /** Nama pemesan — ditampilkan di slot "Booked" pada tabel jadwal */
    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    /** Nomor telepon pemesan (opsional, untuk keperluan operasional) */
    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    /** Tanggal main */
    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    /**
     * Jam mulai slot (07:00 – 21:00).
     * Durasi selalu 1 jam, sehingga jam selesai = startTime + 1 jam.
     */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
