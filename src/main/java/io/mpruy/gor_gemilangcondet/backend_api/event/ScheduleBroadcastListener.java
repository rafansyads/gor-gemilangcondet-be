package io.mpruy.gor_gemilangcondet.backend_api.event;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import io.mpruy.gor_gemilangcondet.backend_api.dto.message.ScheduleUpdateMessage;
import io.mpruy.gor_gemilangcondet.backend_api.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Mendengarkan {@link BookingStatusChangedEvent} dan mem-broadcast pesan ke
 * semua klien WebSocket yang berlangganan topik jadwal tanggal bersangkutan.
 *
 * <h3>Alur broadcast</h3>
 * <pre>
 * BookingStatusChangedEvent
 *   → ScheduleBroadcastListener.onBookingChanged()
 *     → SimpMessagingTemplate.convertAndSend("/topic/schedule/{date}", message)
 *       → semua klien yang subscribe topik tersebut menerima update
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleBroadcastListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ScheduleService       scheduleService;
    private final ObjectMapper          objectMapper;

    /**
     * Broadcast perubahan satu slot ke topik {@code /topic/schedule/{date}}.
     *
     * <p>Payload dikirim sebagai JSON string (text/plain) agar kompatibel
     * dengan berbagai STOMP client termasuk klien browser dan test client.
     *
     * <p>Diberi {@code @Async} agar tidak memblokir thread transaksi utama.
     */
    @Async
    @EventListener
    public void onBookingChanged(BookingStatusChangedEvent event) {
        ScheduleUpdateMessage message = scheduleService.buildUpdateMessage(event.getBooking());

        String topic = "/topic/schedule/" + message.getDate();
        try {
            String json = objectMapper.writeValueAsString(message);
            messagingTemplate.convertAndSend(topic, json);
        } catch (JacksonException e) {
            log.error("[SCHEDULE WS] Gagal serialisasi pesan: {}", e.getMessage(), e);
            return;
        }

        log.info("[SCHEDULE WS] Broadcast → {} | court={} time={} status={}",
                topic, message.getCourtId(), message.getTime(), message.getStatus());
    }
}
