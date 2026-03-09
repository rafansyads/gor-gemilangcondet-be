package io.mpruy.gor_gemilangcondet.backend_api.controller;

import tools.jackson.databind.ObjectMapper;
import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseRequestDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.requests.CreateReservasiRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.requests.RescheduleReservasiRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.*;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.LapanganType;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.ReservasiStatus;
import io.mpruy.gor_gemilangcondet.backend_api.service.ReservasiService;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReservasiController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(CacheAutoConfiguration.class)
@TestPropertySource(properties = "spring.cache.type=none")
class ReservasiControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReservasiService reservasiService;
    @MockitoBean
    private JwtTokenFilter jwtTokenFilter;
    @MockitoBean
    private JwtUtils jwtUtils;
    @MockitoBean
    private JwtTokenBlacklist jwtTokenBlacklist;

    private ReservasiResponse buildResponse() {
        return ReservasiResponse.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .lapanganId(UUID.randomUUID())
                .lapanganName("Badminton 1")
                .lapanganType(LapanganType.BADMINTON)
                .reservationStart(LocalDateTime.now().plusDays(1))
                .reservationEnd(LocalDateTime.now().plusDays(1).plusHours(2))
                .durationInHours(2)
                .namaWakil("John")
                .nomorTelepon("08123")
                .jumlahOrang(4)
                .totalPayment(100000)
                .status(ReservasiStatus.BELUM_DIBAYAR)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("GET /bookings → 200 OK")
    void getAllBookings_Success() throws Exception {
        when(reservasiService.getAllReservations()).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].lapanganName").value("Badminton 1"));
    }

    @Test
    @DisplayName("GET /bookings/{id} → 200 OK")
    void getBookingById_Success() throws Exception {
        UUID id = UUID.randomUUID();
        ReservasiResponse response = buildResponse();
        when(reservasiService.getReservationById(id)).thenReturn(response);

        mockMvc.perform(get("/bookings/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lapanganName").value("Badminton 1"));
    }

    @Test
    @DisplayName("GET /bookings/user/{userId} → 200 OK")
    void getBookingsByUser_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        when(reservasiService.getReservationsByUserId(userId)).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/bookings/user/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].lapanganName").value("Badminton 1"));
    }

    @Test
    @DisplayName("GET /bookings/availability → 200 OK")
    void getAvailability_Success() throws Exception {
        CourtAvailabilityResponse avail = CourtAvailabilityResponse.builder()
                .lapanganId(UUID.randomUUID())
                .lapanganName("Badminton 1")
                .lapanganType(LapanganType.BADMINTON)
                .slots(List.of())
                .build();
        when(reservasiService.getAvailability(any(LocalDate.class), any())).thenReturn(List.of(avail));

        mockMvc.perform(get("/bookings/availability")
                .param("date", LocalDate.now().plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].lapanganName").value("Badminton 1"));
    }

    @Test
    @DisplayName("GET /bookings/equipment-availability → 200 OK")
    void getEquipmentAvailability_Success() throws Exception {
        when(reservasiService.getEquipmentAvailability(any(), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/bookings/equipment-availability")
                .param("courtType", "BADMINTON")
                .param("start", LocalDateTime.now().plusDays(1).toString())
                .param("end", LocalDateTime.now().plusDays(1).plusHours(2).toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /bookings/reserve → 201 Created")
    void createReservation_Success() throws Exception {
        CreateReservasiRequest createReq = new CreateReservasiRequest();
        createReq.setLapanganId(UUID.randomUUID());
        createReq.setReservationStart(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0));
        createReq.setDurationInHours(2);
        createReq.setUserId(UUID.randomUUID());
        createReq.setNamaWakil("John");
        createReq.setNomorTelepon("08123");
        createReq.setJumlahOrang(4);
        BaseRequestDto<CreateReservasiRequest> request = new BaseRequestDto<>();
        request.setData(createReq);

        ReservasiResponse response = buildResponse();
        when(reservasiService.createReservation(any())).thenReturn(response);

        mockMvc.perform(post("/bookings/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("PUT /bookings/reschedule/{id} → 200 OK")
    void rescheduleReservation_Success() throws Exception {
        UUID id = UUID.randomUUID();
        RescheduleReservasiRequest rescheduleReq = new RescheduleReservasiRequest();
        rescheduleReq.setNewReservationStart(LocalDateTime.now().plusDays(2).withHour(14).withMinute(0).withSecond(0));
        rescheduleReq.setDurationInHours(2);
        BaseRequestDto<RescheduleReservasiRequest> request = new BaseRequestDto<>();
        request.setData(rescheduleReq);

        ReservasiResponse response = buildResponse();
        when(reservasiService.rescheduleReservation(eq(id), any())).thenReturn(response);

        mockMvc.perform(put("/bookings/reschedule/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
