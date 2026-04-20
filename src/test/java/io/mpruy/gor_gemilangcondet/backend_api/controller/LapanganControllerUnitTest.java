package io.mpruy.gor_gemilangcondet.backend_api.controller;

import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseRequestDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseResponseDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.requests.CreateLapanganRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.requests.UpdateLapanganRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.LapanganLogResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.LapanganResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.LapanganStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.LapanganType;
import io.mpruy.gor_gemilangcondet.backend_api.service.ReservasiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LapanganControllerUnitTest {

    @Mock
    private ReservasiService reservasiService;

    @InjectMocks
    private LapanganController lapanganController;

    private LapanganResponse buildResponse(UUID id) {
        return LapanganResponse.builder()
                .id(id)
                .name("Badminton 1")
                .kode("BDM-001")
                .type(LapanganType.BADMINTON)
                .status(LapanganStatus.TERSEDIA)
                .tarifPerJam(50000)
                .build();
    }

    @Test
    @DisplayName("getCourts should use getAllCourts when type is null")
    void getCourts_All() {
        when(reservasiService.getAllCourts()).thenReturn(List.of(buildResponse(UUID.randomUUID())));

        ResponseEntity<BaseResponseDto<List<LapanganResponse>>> response = lapanganController.getCourts(null);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
        verify(reservasiService).getAllCourts();
    }

    @Test
    @DisplayName("getCourts should use getCourtsByType when type is provided")
    void getCourts_ByType() {
        when(reservasiService.getCourtsByType(LapanganType.BADMINTON))
                .thenReturn(List.of(buildResponse(UUID.randomUUID())));

        ResponseEntity<BaseResponseDto<List<LapanganResponse>>> response =
                lapanganController.getCourts(LapanganType.BADMINTON);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
        verify(reservasiService).getCourtsByType(LapanganType.BADMINTON);
    }

    @Test
    @DisplayName("createCourt should return 201")
    void createCourt_Success() {
        UUID id = UUID.randomUUID();
        CreateLapanganRequest dto = new CreateLapanganRequest();
        dto.setName("Badminton 4");
        dto.setKode("BDM-004");
        dto.setType(LapanganType.BADMINTON);
        dto.setTarifPerJam(100000);

        BaseRequestDto<CreateLapanganRequest> request = new BaseRequestDto<>();
        request.setData(dto);

        when(reservasiService.createCourt(dto)).thenReturn(buildResponse(id));

        ResponseEntity<BaseResponseDto<LapanganResponse>> response = lapanganController.createCourt(request);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(id, response.getBody().getData().getId());
    }

    @Test
    @DisplayName("getCourt should return 200")
    void getCourt_Success() {
        UUID id = UUID.randomUUID();
        when(reservasiService.getCourt(id)).thenReturn(buildResponse(id));

        ResponseEntity<BaseResponseDto<LapanganResponse>> response = lapanganController.getCourt(id);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(id, response.getBody().getData().getId());
    }

    @Test
    @DisplayName("uploadCourtImage should return 200")
    void uploadCourtImage_Success() {
        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "court.jpg", "image/jpeg", "img".getBytes());
        when(reservasiService.uploadCourtImage(id, file)).thenReturn(buildResponse(id));

        ResponseEntity<BaseResponseDto<LapanganResponse>> response = lapanganController.uploadCourtImage(id, file);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(id, response.getBody().getData().getId());
    }

    @Test
    @DisplayName("getCourtImage should return 404 when file is missing")
    void getCourtImage_NotFound() throws Exception {
        ResponseEntity<Resource> response = lapanganController.getCourtImage("missing-image-file.jpg");

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    @DisplayName("getCourtImage should return png content type")
    void getCourtImage_PngContentType() throws Exception {
        String filename = "court-test-image.png";
        Path path = Paths.get("uploads/court-images").resolve(filename);
        Files.createDirectories(path.getParent());
        Files.write(path, "img".getBytes());

        try {
            ResponseEntity<Resource> response = lapanganController.getCourtImage(filename);
            assertEquals(200, response.getStatusCode().value());
            assertEquals("image/png", response.getHeaders().getContentType().toString());
        } finally {
            Files.deleteIfExists(path);
        }
    }

    @Test
    @DisplayName("getCourtImage should return webp and gif content types")
    void getCourtImage_WebpAndGifContentTypes() throws Exception {
        String[] names = { "court-type.webp", "court-type.gif" };
        String[] expectedTypes = { "image/webp", "image/gif" };

        for (int i = 0; i < names.length; i++) {
            Path path = Paths.get("uploads/court-images").resolve(names[i]);
            Files.createDirectories(path.getParent());
            Files.write(path, "img".getBytes());
            try {
                ResponseEntity<Resource> response = lapanganController.getCourtImage(names[i]);
                assertEquals(200, response.getStatusCode().value());
                assertEquals(expectedTypes[i], response.getHeaders().getContentType().toString());
            } finally {
                Files.deleteIfExists(path);
            }
        }
    }

    @Test
    @DisplayName("getCourtImage should default to jpeg")
    void getCourtImage_DefaultJpeg() throws Exception {
        String filename = "court-default.jpg";
        Path path = Paths.get("uploads/court-images").resolve(filename);
        Files.createDirectories(path.getParent());
        Files.write(path, "img".getBytes());

        try {
            ResponseEntity<Resource> response = lapanganController.getCourtImage(filename);
            assertEquals(200, response.getStatusCode().value());
            assertEquals("image/jpeg", response.getHeaders().getContentType().toString());
            assertTrue(response.getHeaders().getFirst("Content-Disposition").contains(filename));
        } finally {
            Files.deleteIfExists(path);
        }
    }

    @Test
    @DisplayName("updateCourt should return 200")
    void updateCourt_Success() {
        UUID id = UUID.randomUUID();
        UpdateLapanganRequest dto = new UpdateLapanganRequest();
        dto.setName("Updated Court");
        dto.setType(LapanganType.BADMINTON);
        dto.setTarifPerJam(120000);

        BaseRequestDto<UpdateLapanganRequest> request = new BaseRequestDto<>();
        request.setData(dto);

        when(reservasiService.updateCourt(id, dto)).thenReturn(buildResponse(id));

        ResponseEntity<BaseResponseDto<LapanganResponse>> response = lapanganController.updateCourt(id, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(id, response.getBody().getData().getId());
    }

    @Test
    @DisplayName("deleteCourt should return 200")
    void deleteCourt_Success() {
        UUID id = UUID.randomUUID();

        ResponseEntity<BaseResponseDto<Object>> response = lapanganController.deleteCourt(id);

        assertEquals(200, response.getStatusCode().value());
        verify(reservasiService).deleteCourt(id);
    }

    @Test
    @DisplayName("updateCourtStatus should return 200")
    void updateCourtStatus_Success() {
        UUID id = UUID.randomUUID();
        when(reservasiService.updateCourtStatus(id, LapanganStatus.DALAM_PERBAIKAN))
                .thenReturn(buildResponse(id));

        ResponseEntity<BaseResponseDto<LapanganResponse>> response =
                lapanganController.updateCourtStatus(id, LapanganStatus.DALAM_PERBAIKAN);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(id, response.getBody().getData().getId());
    }

    @Test
    @DisplayName("getCourtLogs should return 200")
    void getCourtLogs_Success() {
        UUID id = UUID.randomUUID();
        LapanganLogResponse log = LapanganLogResponse.builder()
                .id(UUID.randomUUID())
                .lapanganId(id)
                .userId(UUID.randomUUID())
                .username("admin")
                .changeDescription("Updated tarif")
                .createdAt(LocalDateTime.now())
                .build();

        when(reservasiService.getCourtLogs(id)).thenReturn(List.of(log));

        ResponseEntity<BaseResponseDto<List<LapanganLogResponse>>> response = lapanganController.getCourtLogs(id);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
    }
}
