package io.mpruy.gor_gemilangcondet.backend_api.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import io.mpruy.gor_gemilangcondet.backend_api.entities.bookings.Booking;

/**
 * Event yang diterbitkan oleh {@link io.mpruy.gor_gemilangcondet.backend_api.service.ScheduleService}
 * setiap kali ada perubahan status booking (baru dibuat, dikonfirmasi, dibatalkan).
 *
 * <p>Listener {@link ScheduleBroadcastListener} menangkap event ini dan mem-broadcast
 * perubahan ke semua klien WebSocket yang berlangganan topik jadwal.
 */
@Getter
public class BookingStatusChangedEvent extends ApplicationEvent {

    private final Booking booking;

    public BookingStatusChangedEvent(Object source, Booking booking) {
        super(source);
        this.booking = booking;
    }
}
