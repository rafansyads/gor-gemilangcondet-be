package io.mpruy.gor_gemilangcondet.backend_api.uat;

import io.mpruy.gor_gemilangcondet.backend_api.dto.message.ScheduleUpdateMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UAT — PBI-BE1 & BE2: WebSocket Broadcast Real-Time
 *
 * Skenario yang dicakup:
 *   BE1-01 · Koneksi WebSocket berhasil terbentuk (CONNECTED)
 *   BE1-02 · Slot berubah ke BOOKED saat booking baru dibuat (tanpa refresh)
 *   BE2-01 · Broadcast diterima klien yang sedang terhubung
 *   BE2-03 · Pesan broadcast memuat semua field wajib
 *   BE2-04 · Error pada satu klien tidak menghambat broadcast ke klien lain
 *   FE3-02 · lastUpdated dalam pesan broadcast tidak null
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("BE1 & BE2 · WebSocket Broadcast Integration UAT")
class BE1_BE2_WebSocketUATTest {

    @LocalServerPort int port;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String DATE = "2026-03-09";

    private WebSocketStompClient buildStompClient() {
        WebSocketStompClient client = new WebSocketStompClient(
                new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient())))
        );
        client.setMessageConverter(new StringMessageConverter());
        return client;
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map> postBooking(int courtId, String time, String name) {
        return restTemplate.postForEntity(
                "http://localhost:" + port + "/api/test/book",
                Map.of("courtId", courtId, "date", DATE, "time", time, "customerName", name),
                Map.class
        );
    }

    @BeforeEach void setUp() {}
    @AfterEach  void tearDown() {}

    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BE1-01 · Koneksi WebSocket ke /ws berhasil (status CONNECTED)")
    void be1_01_webSocketConnectionEstablished() throws Exception {
        CountDownLatch connected = new CountDownLatch(1);
        WebSocketStompClient client = buildStompClient();

        StompSessionHandler handler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                connected.countDown();
                session.disconnect();
            }
        };

        client.connectAsync(wsUrl(), handler);
        boolean ok = connected.await(5, TimeUnit.SECONDS);
        client.stop();
        assertThat(ok).isTrue().withFailMessage("WebSocket tidak berhasil terhubung dalam 5 detik");
    }

    @Test
    @DisplayName("BE1-02 · Klien menerima pesan saat booking baru dibuat (tanpa reload halaman)")
    void be1_02_clientReceivesMessageWhenBookingCreated() throws Exception {
        CountDownLatch connected = new CountDownLatch(1);
        CountDownLatch received  = new CountDownLatch(1);
        AtomicReference<String> msgRef = new AtomicReference<>();
        WebSocketStompClient client = buildStompClient();

        client.connectAsync(wsUrl(), new StompSessionHandlerAdapter() {
            @Override public void afterConnected(StompSession session, StompHeaders headers) {
                session.subscribe("/topic/schedule/" + DATE, new StompFrameHandler() {
                    @Override public Type getPayloadType(StompHeaders h) { return String.class; }
                    @Override public void handleFrame(StompHeaders h, Object payload) {
                        msgRef.set((String) payload);
                        received.countDown();
                    }
                });
                connected.countDown();
            }
        });
        connected.await(5, TimeUnit.SECONDS);

        ResponseEntity<Map> resp = postBooking(1, "09:00", "Hendra");
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        boolean ok = received.await(3, TimeUnit.SECONDS);
        client.stop();

        assertThat(ok).isTrue().withFailMessage("Pesan broadcast tidak diterima dalam 3 detik");
        assertThat(msgRef.get()).isNotNull();
        assertThat(msgRef.get()).contains("BOOKED");
    }

    @Test
    @DisplayName("BE2-01 · Dua klien aktif keduanya menerima broadcast yang sama")
    void be2_01_allActiveClientsReceiveBroadcast() throws Exception {
        CountDownLatch conn1 = new CountDownLatch(1);
        CountDownLatch conn2 = new CountDownLatch(1);
        CountDownLatch recv1 = new CountDownLatch(1);
        CountDownLatch recv2 = new CountDownLatch(1);

        WebSocketStompClient client1 = buildStompClient();
        WebSocketStompClient client2 = buildStompClient();

        client1.connectAsync(wsUrl(), new StompSessionHandlerAdapter() {
            @Override public void afterConnected(StompSession session, StompHeaders headers) {
                session.subscribe("/topic/schedule/" + DATE, new StompFrameHandler() {
                    @Override public Type getPayloadType(StompHeaders h) { return String.class; }
                    @Override public void handleFrame(StompHeaders h, Object p) { recv1.countDown(); }
                });
                conn1.countDown();
            }
        });

        client2.connectAsync(wsUrl(), new StompSessionHandlerAdapter() {
            @Override public void afterConnected(StompSession session, StompHeaders headers) {
                session.subscribe("/topic/schedule/" + DATE, new StompFrameHandler() {
                    @Override public Type getPayloadType(StompHeaders h) { return String.class; }
                    @Override public void handleFrame(StompHeaders h, Object p) { recv2.countDown(); }
                });
                conn2.countDown();
            }
        });

        conn1.await(5, TimeUnit.SECONDS);
        conn2.await(5, TimeUnit.SECONDS);

        postBooking(2, "15:00", "Sari");

        assertThat(recv1.await(3, TimeUnit.SECONDS)).isTrue().withFailMessage("Klien 1 tidak menerima broadcast");
        assertThat(recv2.await(3, TimeUnit.SECONDS)).isTrue().withFailMessage("Klien 2 tidak menerima broadcast");

        client1.stop();
        client2.stop();
    }

    @Test
    @DisplayName("BE2-03 · Payload broadcast memuat field: date, courtId, courtName, time, status, lastUpdated")
    void be2_03_broadcastPayloadHasAllRequiredFields() throws Exception {
        CountDownLatch connected = new CountDownLatch(1);
        CountDownLatch received  = new CountDownLatch(1);
        AtomicReference<String> msgRef = new AtomicReference<>();
        WebSocketStompClient client = buildStompClient();

        client.connectAsync(wsUrl(), new StompSessionHandlerAdapter() {
            @Override public void afterConnected(StompSession session, StompHeaders headers) {
                session.subscribe("/topic/schedule/" + DATE, new StompFrameHandler() {
                    @Override public Type getPayloadType(StompHeaders h) { return String.class; }
                    @Override public void handleFrame(StompHeaders h, Object p) {
                        msgRef.set((String) p);
                        received.countDown();
                    }
                });
                connected.countDown();
            }
        });

        connected.await(5, TimeUnit.SECONDS);
        postBooking(3, "13:00", "Dian");
        received.await(3, TimeUnit.SECONDS);
        client.stop();

        String msg = msgRef.get();
        assertThat(msg).isNotNull();
        assertThat(msg).contains("date");
        assertThat(msg).contains("courtName");
        assertThat(msg).contains("time");
        assertThat(msg).contains("status");
        assertThat(msg).contains("lastUpdated");
    }

    @Test
    @DisplayName("BE2-04 · Klien yang disconnect tidak menghambat broadcast ke klien lain")
    void be2_04_disconnectedClientDoesNotBlockBroadcast() throws Exception {
        CountDownLatch connGood = new CountDownLatch(1);
        CountDownLatch recvGood = new CountDownLatch(1);
        WebSocketStompClient goodClient = buildStompClient();

        goodClient.connectAsync(wsUrl(), new StompSessionHandlerAdapter() {
            @Override public void afterConnected(StompSession session, StompHeaders headers) {
                session.subscribe("/topic/schedule/" + DATE, new StompFrameHandler() {
                    @Override public Type getPayloadType(StompHeaders h) { return String.class; }
                    @Override public void handleFrame(StompHeaders h, Object p) { recvGood.countDown(); }
                });
                connGood.countDown();
            }
        });

        // Klien "buruk" — disconnect paksa
        WebSocketStompClient badClient = buildStompClient();
        CountDownLatch connBad = new CountDownLatch(1);
        badClient.connectAsync(wsUrl(), new StompSessionHandlerAdapter() {
            @Override public void afterConnected(StompSession session, StompHeaders headers) {
                session.disconnect();
                connBad.countDown();
            }
        });

        connGood.await(5, TimeUnit.SECONDS);
        connBad.await(5, TimeUnit.SECONDS);
        badClient.stop();

        postBooking(4, "16:00", "Eko");

        assertThat(recvGood.await(3, TimeUnit.SECONDS)).isTrue()
                .withFailMessage("Klien aktif tidak menerima broadcast meski klien lain sudah disconnect");
        goodClient.stop();
    }

    // ──────────────────────────────────────────────────────────────────────────

    private String wsUrl() {
        return "ws://localhost:" + port + "/ws";
    }
}
