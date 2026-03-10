package io.mpruy.gor_gemilangcondet.backend_api.controller;

import tools.jackson.databind.ObjectMapper;
import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseRequestDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.requests.CreateLapanganRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.requests.UpdateLapanganRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.LapanganResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.LapanganStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.LapanganType;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LapanganController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(CacheAutoConfiguration.class)
@TestPropertySource(properties = "spring.cache.type=none")
class LapanganControllerTest {

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

    private LapanganResponse buildResponse() {
        return LapanganResponse.builder()
                .id(UUID.randomUUID())
                .name("Badminton 1")
                .type(LapanganType.BADMINTON)
                .status(LapanganStatus.TERSEDIA)
                .tarifPerJam(50000)
                .build();
    }

    @Test
    @DisplayName("GET /courts → 200 OK")
    void getCourts_All_Success() throws Exception {
        when(reservasiService.getAllCourts()).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/courts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Badminton 1"));
    }

    @Test
    @DisplayName("GET /courts?type=BADMINTON → 200 OK (filtered)")
    void getCourts_ByType_Success() throws Exception {
        when(reservasiService.getCourtsByType(LapanganType.BADMINTON))
                .thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/courts").param("type", "BADMINTON"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].type").value("BADMINTON"));
    }

    @Test
    @DisplayName("POST /courts → 201 Created")
    void createCourt_Success() throws Exception {
        CreateLapanganRequest createReq = new CreateLapanganRequest();
        createReq.setName("Futsal 1");
        createReq.setType(LapanganType.FUTSAL);
        createReq.setTarifPerJam(100000);
        BaseRequestDto<CreateLapanganRequest> request = new BaseRequestDto<>();
        request.setData(createReq);

        LapanganResponse resp = buildResponse();
        resp.setName("Futsal 1");
        resp.setType(LapanganType.FUTSAL);
        when(reservasiService.createCourt(any())).thenReturn(resp);

        mockMvc.perform(post("/courts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Futsal 1"));
    }

    @Test
    @DisplayName("PUT /courts/{id} → 200 OK")
    void updateCourt_Success() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateLapanganRequest updateReq = new UpdateLapanganRequest();
        updateReq.setName("Updated");
        updateReq.setType(LapanganType.BASKET);
        updateReq.setTarifPerJam(200000);
        BaseRequestDto<UpdateLapanganRequest> request = new BaseRequestDto<>();
        request.setData(updateReq);

        LapanganResponse resp = buildResponse();
        resp.setName("Updated");
        when(reservasiService.updateCourt(eq(id), any())).thenReturn(resp);

        mockMvc.perform(put("/courts/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated"));
    }

    @Test
    @DisplayName("DELETE /courts/{id} → 200 OK")
    void deleteCourt_Success() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(reservasiService).deleteCourt(id);

        mockMvc.perform(delete("/courts/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Lapangan berhasil dihapus"));
    }

    @Test
    @DisplayName("PATCH /courts/{id}/status → 200 OK")
    void updateCourtStatus_Success() throws Exception {
        UUID id = UUID.randomUUID();
        LapanganResponse resp = buildResponse();
        resp.setStatus(LapanganStatus.DALAM_PERBAIKAN);
        when(reservasiService.updateCourtStatus(eq(id), eq(LapanganStatus.DALAM_PERBAIKAN)))
                .thenReturn(resp);

        mockMvc.perform(patch("/courts/" + id + "/status")
                .param("status", "DALAM_PERBAIKAN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DALAM_PERBAIKAN"));
    }
}
