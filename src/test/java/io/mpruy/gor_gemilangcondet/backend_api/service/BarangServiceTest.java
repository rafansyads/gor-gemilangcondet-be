package io.mpruy.gor_gemilangcondet.backend_api.service;

import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.BarangRequestDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.BarangResponseDto;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.Barang;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.toko.BarangToko;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.toko.BarangTokoStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.toko.BarangTokoType;
import io.mpruy.gor_gemilangcondet.backend_api.repository.BarangRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.BarangTokoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BarangServiceTest {

    @Mock
    private BarangRepository barangRepository;
    @Mock
    private BarangTokoRepository barangTokoRepository;

    @InjectMocks
    private BarangService barangService;

    private BarangRequestDto buildRequest() {
        return BarangRequestDto.builder()
                .name("Shuttlecock")
                .price(45000)
                .stock(12)
                .unit("tube")
                .build();
    }

    @Test
    @DisplayName("createBarang should persist product without image")
    void createBarang_WithoutImage_Success() throws IOException {
        BarangRequestDto request = buildRequest();

        when(barangTokoRepository.save(any(BarangToko.class))).thenAnswer(invocation -> {
            BarangToko barang = invocation.getArgument(0);
            barang.setId(UUID.randomUUID());
            return barang;
        });

        BarangResponseDto response = barangService.createBarang(request, null);

        assertNotNull(response.getId());
        assertEquals("Shuttlecock", response.getName());
        assertEquals(45000, response.getPrice());
        assertEquals(12, response.getStock());
        assertNull(response.getImageUrl());
    }

    @Test
    @DisplayName("createBarang should persist product with uploaded image")
    void createBarang_WithImage_Success() throws IOException {
        BarangRequestDto request = buildRequest();
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "shuttlecock.png",
                "image/png",
                "fake-image".getBytes());

        when(barangTokoRepository.save(any(BarangToko.class))).thenAnswer(invocation -> {
            BarangToko barang = invocation.getArgument(0);
            barang.setId(UUID.randomUUID());
            return barang;
        });

        BarangResponseDto response = barangService.createBarang(request, image);

        assertNotNull(response.getImageUrl());
        assertEquals("Shuttlecock", response.getName());

        Path uploadedPath = Path.of(response.getImageUrl().substring(1));
        Files.deleteIfExists(uploadedPath);
    }

    @Test
    @DisplayName("getAllBarang should map all products")
    void getAllBarang_Success() {
        Barang first = BarangToko.builder()
                .id(UUID.randomUUID())
                .name("Raket")
                .price(500000)
                .stock(7)
                .unit("pcs")
                .type(BarangTokoType.ALAT_OLAHRAGA)
                .status(BarangTokoStatus.TERSEDIA)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Barang second = BarangToko.builder()
                .id(UUID.randomUUID())
                .name("Grip")
                .price(10000)
                .stock(20)
                .unit("pcs")
                .type(BarangTokoType.LAINNYA)
                .status(BarangTokoStatus.TERSEDIA)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(barangRepository.findAll()).thenReturn(List.of(first, second));

        List<BarangResponseDto> result = barangService.getAllBarang();

        assertEquals(2, result.size());
        assertEquals("Raket", result.get(0).getName());
        assertEquals("Grip", result.get(1).getName());
    }
}
