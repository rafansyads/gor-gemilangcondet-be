package io.mpruy.gor_gemilangcondet.backend_api.service;

import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.BarangRequestDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.BarangResponseDto;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.Barang;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.kantin.BarangKantin;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.kantin.BarangKantinStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.kantin.BarangKantinType;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.toko.BarangToko;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.toko.BarangTokoStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.toko.BarangTokoType;
import io.mpruy.gor_gemilangcondet.backend_api.repository.BarangKantinRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.BarangRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.BarangTokoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final BarangKantinRepository barangKantinRepository;

    private final String PRODUCT_UPLOAD_DIR = "uploads/products";

    @Transactional
    public BarangResponseDto createBarang(BarangRequestDto request, MultipartFile image) throws IOException {
        validateCategoryAccess(request.getCategory());
        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            File uploadDir = new File(PRODUCT_UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            String filename = UUID.randomUUID() + "_" + image.getOriginalFilename();
            Path filePath = Paths.get(PRODUCT_UPLOAD_DIR, filename);
            Files.copy(image.getInputStream(), filePath);
            imageUrl = filename;
        }

        String category = request.getCategory() != null ? request.getCategory().toUpperCase() : "AKSESORIS";
        LocalDateTime now = LocalDateTime.now();
        Barang saved;

        if (category.equals("MAKANAN") || category.equals("MINUMAN")) {
            BarangKantinType kantinType = category.equals("MAKANAN")
                    ? BarangKantinType.MAKANAN
                    : BarangKantinType.MINUMAN;
            BarangKantin barang = BarangKantin.builder()
                    .name(request.getName())
                    .sku(request.getSku())
                    .purchasePrice(request.getPurchasePrice())
                    .price(request.getPrice())
                    .stock(request.getStock())
                    .unit(request.getUnit())
                    .imageUrl(imageUrl)
                    .type(kantinType)
                    .status(BarangKantinStatus.TERSEDIA)
                    .reorderThreshold(10)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            saved = barangKantinRepository.save(barang);
        } else {
            BarangTokoType tokoType = switch (category) {
                case "SHUTTLECOCK" -> BarangTokoType.BOLA;
                case "RAKET_SENAR" -> BarangTokoType.ALAT_OLAHRAGA;
                default -> BarangTokoType.LAINNYA;
            };
            BarangToko barang = BarangToko.builder()
                    .name(request.getName())
                    .sku(request.getSku())
                    .purchasePrice(request.getPurchasePrice())
                    .price(request.getPrice())
                    .stock(request.getStock())
                    .unit(request.getUnit())
                    .imageUrl(imageUrl)
                    .type(tokoType)
                    .status(BarangTokoStatus.TERSEDIA)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            saved = barangTokoRepository.save(barang);
        }

        return mapToDto(saved);
    }

    public List<BarangResponseDto> getAllBarang() {
        return barangRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<Barang> getAllSellable() {
        return barangRepository.findAllSellable();
    }

    public Barang getBarangById(UUID id) {
        return barangRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Barang dengan ID " + id + " tidak ditemukan"));
    }

    @Transactional
    public Barang updateBarang(UUID id, BarangRequestDto request, MultipartFile image) throws IOException {
        validateCategoryAccess(request.getCategory());
        Barang barang = barangRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Barang dengan ID " + id + " tidak ditemukan"));

        barang.setName(request.getName());
        barang.setPrice(request.getPrice());
        barang.setStock(request.getStock());
        barang.setUnit(request.getUnit());
        barang.setSku(request.getSku());
        barang.setPurchasePrice(request.getPurchasePrice());

        if (image != null && !image.isEmpty()) {
            File uploadDir = new File(PRODUCT_UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            String filename = UUID.randomUUID() + "_" + image.getOriginalFilename();
            Path filePath = Paths.get(PRODUCT_UPLOAD_DIR, filename);
            Files.copy(image.getInputStream(), filePath);
            barang.setImageUrl(filename);
        }

        barang.setUpdatedAt(LocalDateTime.now());
        return barangRepository.save(barang);
    }

    @Transactional
    public void deleteBarang(UUID id) {
        Barang barang = barangRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Barang dengan ID " + id + " tidak ditemukan"));
        validateCategoryAccess(resolveCategory(barang));
        barangRepository.delete(barang);
    }

    private void validateCategoryAccess(String category) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return;

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));
        if (isAdmin) return;

        boolean isStafLapangan = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("STAF_LAPANGAN"));
        boolean isStafToko = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("STAF_TOKO"));

        String cat = category != null ? category.toUpperCase() : "AKSESORIS";
        boolean isKantin    = cat.equals("MAKANAN") || cat.equals("MINUMAN");
        boolean isOlahraga  = cat.equals("SHUTTLECOCK") || cat.equals("RAKET_SENAR");
        boolean isAksesoris = cat.equals("AKSESORIS");

        // STAF_LAPANGAN: alat olahraga + barang toko (AKSESORIS)
        // STAF_TOKO: barang kantin (MAKANAN, MINUMAN)
        if (isKantin && !isStafToko) {
            throw new AccessDeniedException("Hanya Staf Toko yang dapat mengelola barang kantin");
        }
        if (isOlahraga && !isStafLapangan && !isStafToko) {
            throw new AccessDeniedException("Akses ditolak untuk kategori ini");
        }
        if (isAksesoris && !isStafLapangan) {
            throw new AccessDeniedException("Hanya Staf Lapangan yang dapat mengelola aksesoris");
        }
    }

    private String resolveCategory(Barang barang) {
        if (barang instanceof BarangKantin kantin) {
            return switch (kantin.getType()) {
                case MINUMAN -> "MINUMAN";
                default -> "MAKANAN";
            };
        } else if (barang instanceof BarangToko toko) {
            return switch (toko.getType()) {
                case BOLA -> "SHUTTLECOCK";
                case ALAT_OLAHRAGA -> "RAKET_SENAR";
                default -> "AKSESORIS";
            };
        }
        return "AKSESORIS";
    }

    private BarangResponseDto mapToDto(Barang barang) {
        return BarangResponseDto.builder()
                .id(barang.getId())
                .name(barang.getName())
                .category(resolveCategory(barang))
                .sku(barang.getSku())
                .purchasePrice(barang.getPurchasePrice() != null ? barang.getPurchasePrice() : 0.0)
                .price(barang.getPrice())
                .stock(barang.getStock())
                .unit(barang.getUnit())
                .imageUrl(barang.getImageUrl())
                .build();
    }
}
