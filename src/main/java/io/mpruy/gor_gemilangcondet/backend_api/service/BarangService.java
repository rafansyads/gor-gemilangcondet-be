package io.mpruy.gor_gemilangcondet.backend_api.service;

import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.BarangRequestDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.BarangResponseDto;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.Barang;
import io.mpruy.gor_gemilangcondet.backend_api.repository.BarangRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.upload.dir:uploads/payment-proofs}")
    private String baseUploadDir;
    
    // We will save in uploads/products
    private final String PRODUCT_UPLOAD_DIR = "uploads/products";

    @Transactional
    public BarangResponseDto createBarang(BarangRequestDto request, MultipartFile image) throws IOException {
        if (request.getPrice() <= request.getPurchasePrice()) {
            throw new IllegalArgumentException("Harga Jual harus lebih besar dari Harga Beli!");
        }

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

        Barang barang = Barang.builder()
                .name(request.getName())
                .sku(request.getSku())
                .type(request.getType())
                .purchasePrice(request.getPurchasePrice())
                .price(request.getPrice())
                .stock(request.getStock())
                .unit(request.getUnit())
                .imageUrl(imageUrl)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Barang saved = barangRepository.save(barang);
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
                .sku(barang.getSku())
                .type(barang.getType())
                .purchasePrice(barang.getPurchasePrice())
                .price(barang.getPrice())
                .stock(barang.getStock())
                .unit(barang.getUnit())
                .imageUrl(barang.getImageUrl())
                .build();
    }
}
