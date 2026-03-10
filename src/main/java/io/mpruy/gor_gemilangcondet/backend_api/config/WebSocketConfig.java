package io.mpruy.gor_gemilangcondet.backend_api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Konfigurasi WebSocket berbasis STOMP untuk fitur Jadwal Real-Time.
 *
 * <h3>Cara kerja</h3>
 * <ul>
 *   <li>FE terhubung ke endpoint {@code /ws} via STOMP (SockJS fallback tersedia).</li>
 *   <li>FE subscribe ke {@code /topic/schedule/{date}} untuk menerima update slot.</li>
 *   <li>BE broadcast ke topik tersebut menggunakan {@link org.springframework.messaging.simp.SimpMessagingTemplate}
 *       melalui {@link io.mpruy.gor_gemilangcondet.backend_api.event.ScheduleBroadcastListener}.</li>
 * </ul>
 *
 * <h3>Contoh penggunaan FE (JavaScript)</h3>
 * <pre>
 * const client = new StompJS.Client({
 *   brokerURL: 'ws://localhost:8080/ws'
 * });
 * client.onConnect = () => {
 *   client.subscribe('/topic/schedule/2024-12-25', (msg) => {
 *     const update = JSON.parse(msg.body);
 *     // perbarui sel (update.courtId, update.time)
 *   });
 * };
 * client.activate();
 * </pre>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Daftarkan broker in-memory untuk prefix {@code /topic}.
     * Prefix {@code /app} digunakan jika FE ingin mengirim pesan ke server
     * (tidak wajib untuk use-case jadwal read-only ini).
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Daftarkan STOMP endpoint {@code /ws} dengan dukungan SockJS
     * sebagai fallback untuk browser yang tidak mendukung WebSocket native.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // ganti dengan domain FE di production
                .withSockJS();
    }
}
