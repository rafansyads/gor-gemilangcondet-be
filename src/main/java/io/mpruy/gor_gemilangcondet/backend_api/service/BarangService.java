package io.mpruy.gor_gemilangcondet.backend_api.service;

import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.BarangRequestDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.BarangResponseDto;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.Barang;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.toko.BarangToko;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.toko.BarangTokoStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.toko.BarangTokoType;
import io.mpruy.gor_gemilangcondet.backend_api.repository.BarangRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.BarangTokoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BarangService {

    private final BarangRepository barangRepository;
    private final BarangTokoRepository barangTokoRepository;

    private final String PRODUCT_UPLOAD_DIR = "uploads/products";

    @Transactional
    public BarangResponseDto createBarang(BarangRequestDto request, MultipartFile image) throws IOException {
        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            File uploadDir = new File(PRODUCT_UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            String filename = UUID.randomUUID() + "_" + image.getOriginalFilename();
            Path filePath = Paths.get(PRODUCT_UPLOAD_DIR, filename);
            Files.copy(image.getInputStream(), filePath);
            imageUrl = "/" + PRODUCT_UPLOAD_DIR + "/" + filename;
        }

        BarangToko barang = BarangToko.builder()
                .name(request.getName())
                .price(request.getPrice())
                .stock(request.getStock())
                .unit(request.getUnit())
                .imageUrl(imageUrl)
                .type(BarangTokoType.LAINNYA)
                .status(BarangTokoStatus.TERSEDIA)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Barang saved = barangTokoRepository.save(barang);
        return mapToDto(saved);
    }

    public List<BarangResponseDto> getAllBarang() {
        return barangRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private BarangResponseDto mapToDto(Barang barang) {
        return BarangResponseDto.builder()
                .id(barang.getId())
                .name(barang.getName())
                .price(barang.getPrice())
                .stock(barang.getStock())
                .unit(barang.getUnit())
                .imageUrl(barang.getImageUrl())
                .build();
    }
}
