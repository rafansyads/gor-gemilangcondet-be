package io.mpruy.gor_gemilangcondet.backend_api.controller;

import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseResponseDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.BarangRequestDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.BarangResponseDto;
import io.mpruy.gor_gemilangcondet.backend_api.service.BarangService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BarangControllerUnitTest {

    @Mock
    private BarangService barangService;

    @InjectMocks
    private BarangController barangController;

    private BarangRequestDto buildRequest() {
        return BarangRequestDto.builder()
                .name("Shuttlecock")
                .price(50000)
                .stock(10)
                .unit("tube")
                .build();
    }

    @Test
    @DisplayName("createBarang should return 201 when service succeeds")
    void createBarang_Success() throws Exception {
        BarangRequestDto request = buildRequest();
        MockMultipartFile image = new MockMultipartFile("image", "prod.png", "image/png", "img".getBytes());

        BarangResponseDto dto = BarangResponseDto.builder()
                .id(UUID.randomUUID())
                .name("Shuttlecock")
                .price(50000)
                .stock(10)
                .unit("tube")
                .imageUrl("/uploads/products/prod.png")
                .build();

        when(barangService.createBarang(request, image)).thenReturn(dto);

        ResponseEntity<BaseResponseDto<BarangResponseDto>> response = barangController.createBarang(request, image);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(dto, response.getBody().getData());
    }

    @Test
    @DisplayName("createBarang should return 400 when service throws IllegalArgumentException")
    void createBarang_BadRequest() throws Exception {
        BarangRequestDto request = buildRequest();
        MockMultipartFile image = new MockMultipartFile("image", "prod.png", "image/png", "img".getBytes());

        when(barangService.createBarang(request, image)).thenThrow(new IllegalArgumentException("Nama tidak valid"));

        ResponseEntity<BaseResponseDto<BarangResponseDto>> response = barangController.createBarang(request, image);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Nama tidak valid", response.getBody().getMessage());
    }

    @Test
    @DisplayName("createBarang should return 500 when service throws IOException")
    void createBarang_InternalError() throws Exception {
        BarangRequestDto request = buildRequest();
        MockMultipartFile image = new MockMultipartFile("image", "prod.png", "image/png", "img".getBytes());

        when(barangService.createBarang(request, image)).thenThrow(new IOException("Disk full"));

        ResponseEntity<BaseResponseDto<BarangResponseDto>> response = barangController.createBarang(request, image);

        assertEquals(500, response.getStatusCode().value());
        assertTrue(response.getBody().getMessage().contains("Disk full"));
    }

    @Test
    @DisplayName("getAllBarang should return 200 with item list")
    void getAllBarang_Success() {
        BarangResponseDto item = BarangResponseDto.builder()
                .id(UUID.randomUUID())
                .name("Grip")
                .price(10000)
                .stock(15)
                .unit("pcs")
                .build();

        when(barangService.getAllBarang()).thenReturn(List.of(item));

        ResponseEntity<BaseResponseDto<List<BarangResponseDto>>> response = barangController.getAllBarang();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
        assertEquals("Grip", response.getBody().getData().get(0).getName());
    }
}
