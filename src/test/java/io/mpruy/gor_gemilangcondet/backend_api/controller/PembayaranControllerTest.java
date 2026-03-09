package io.mpruy.gor_gemilangcondet.backend_api.controller;

import tools.jackson.databind.ObjectMapper;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.ConfirmPaymentResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.PembayaranResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.ReservasiResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.payment.PaymentStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.LapanganType;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.ReservasiStatus;
import io.mpruy.gor_gemilangcondet.backend_api.service.PembayaranService;
import io.mpruy.gor_gemilangcondet.backend_api.security.jwt.JwtTokenFilter;
import io.mpruy.gor_gemilangcondet.backend_api.security.jwt.JwtUtils;
import io.mpruy.gor_gemilangcondet.backend_api.security.service.JwtTokenBlacklist;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PembayaranController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(CacheAutoConfiguration.class)
@TestPropertySource(properties = "spring.cache.type=none")
class PembayaranControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PembayaranService pembayaranService;
    @MockitoBean
    private JwtTokenFilter jwtTokenFilter;
    @MockitoBean
    private JwtUtils jwtUtils;
    @MockitoBean
    private JwtTokenBlacklist jwtTokenBlacklist;

    private ReservasiResponse buildReservasiResponse() {
        return ReservasiResponse.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .lapanganId(UUID.randomUUID())
                .lapanganName("Badminton 1")
                .lapanganType(LapanganType.BADMINTON)
                .reservationStart(LocalDateTime.now().plusDays(1))
                .reservationEnd(LocalDateTime.now().plusDays(1).plusHours(2))
                .durationInHours(2)
                .totalPayment(100000)
                .status(ReservasiStatus.BELUM_DIBAYAR)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("GET /payments/unpaid → 200 OK")
    void getUnpaidReservations_Success() throws Exception {
        when(pembayaranService.getUnpaidReservations()).thenReturn(List.of(buildReservasiResponse()));

        mockMvc.perform(get("/payments/unpaid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].lapanganName").value("Badminton 1"));
    }

    @Test
    @DisplayName("GET /payments/staff-reservations → 200 OK")
    void getStaffReservations_Success() throws Exception {
        when(pembayaranService.getReservationsForStaff()).thenReturn(List.of());

        mockMvc.perform(get("/payments/staff-reservations"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /payments/reservation/{id} → 200 OK")
    void getPaymentByReservation_Success() throws Exception {
        UUID resId = UUID.randomUUID();
        PembayaranResponse payResponse = PembayaranResponse.builder()
                .id(UUID.randomUUID())
                .reservationId(resId)
                .status(PaymentStatus.LUNAS)
                .price(100000)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(pembayaranService.getPaymentByReservationId(resId)).thenReturn(payResponse);

        mockMvc.perform(get("/payments/reservation/" + resId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("LUNAS"));
    }

    @Test
    @DisplayName("POST /payments/{id}/upload-proof → 200 OK")
    void uploadPaymentProof_Success() throws Exception {
        UUID resId = UUID.randomUUID();
        ReservasiResponse resp = buildReservasiResponse();
        resp.setStatus(ReservasiStatus.MENUNGGU_KONFIRMASI_STAF);
        when(pembayaranService.uploadPaymentProof(eq(resId), any())).thenReturn(resp);

        MockMultipartFile file = new MockMultipartFile("file", "receipt.jpg",
                "image/jpeg", "fake image".getBytes());

        mockMvc.perform(multipart("/payments/{reservasiId}/upload-proof", resId)
                .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Bukti pembayaran berhasil diunggah"));
    }

    @Test
    @DisplayName("PUT /payments/confirm/{id} → 200 OK")
    void confirmPayment_Success() throws Exception {
        UUID resId = UUID.randomUUID();
        ConfirmPaymentResponse resp = ConfirmPaymentResponse.builder()
                .reservationId(resId)
                .reservationStatus(ReservasiStatus.DIKONFIRMASI)
                .paymentStatus(PaymentStatus.LUNAS)
                .lapanganName("Badminton 1")
                .message("Pembayaran berhasil dikonfirmasi")
                .build();
        when(pembayaranService.confirmPayment(resId)).thenReturn(resp);

        mockMvc.perform(put("/payments/confirm/" + resId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Pembayaran berhasil dikonfirmasi"));
    }

    @Test
    @DisplayName("PUT /payments/reject/{id} → 200 OK")
    void rejectPayment_Success() throws Exception {
        UUID resId = UUID.randomUUID();
        ConfirmPaymentResponse resp = ConfirmPaymentResponse.builder()
                .reservationId(resId)
                .reservationStatus(ReservasiStatus.DITOLAK)
                .lapanganName("Badminton 1")
                .message("Reservasi ditolak")
                .build();
        when(pembayaranService.rejectPayment(resId)).thenReturn(resp);

        mockMvc.perform(put("/payments/reject/" + resId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Reservasi ditolak"));
    }
}
