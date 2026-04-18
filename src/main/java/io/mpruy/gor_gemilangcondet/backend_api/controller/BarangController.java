package io.mpruy.gor_gemilangcondet.backend_api.controller;

import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseResponseDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.BarangRequestDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.BarangResponseDto;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.Barang;
import io.mpruy.gor_gemilangcondet.backend_api.service.BarangService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/barang")
@RequiredArgsConstructor
public class BarangController {

    private final BarangService barangService;

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<BaseResponseDto<BarangResponseDto>> createBarang(
            @Valid @RequestPart("data") BarangRequestDto request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        try {
            BarangResponseDto response = barangService.createBarang(request, image);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    new BaseResponseDto<>(HttpStatus.CREATED.value(), "Berhasil menambahkan produk", Instant.now(), response)
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    new BaseResponseDto<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), Instant.now(), null)
            );
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new BaseResponseDto<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Gagal mengunggah foto produk: " + e.getMessage(), Instant.now(), null)
            );
        }
    }

    @GetMapping
    public ResponseEntity<BaseResponseDto<List<BarangResponseDto>>> getAllBarang() {
        List<BarangResponseDto> response = barangService.getAllBarang();
        return ResponseEntity.ok(
                new BaseResponseDto<>(HttpStatus.OK.value(), "Berhasil mengambil daftar produk", Instant.now(), response)
        );
    }

    @GetMapping("/image/{filename:.+}")
    public ResponseEntity<Resource> getProductImage(@PathVariable String filename) throws Exception {
        Path filePath = Paths.get("uploads/products").resolve(filename).normalize();
        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        String lower = filename.toLowerCase();
        String contentType = "image/jpeg";
        if (lower.endsWith(".png")) contentType = "image/png";
        else if (lower.endsWith(".webp")) contentType = "image/webp";
        else if (lower.endsWith(".gif")) contentType = "image/gif";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(resource);
    }

    @GetMapping("/sellable")
    public ResponseEntity<BaseResponseDto<List<Barang>>> getAllSellable() {
        List<Barang> response = barangService.getAllSellable();
        return ResponseEntity.ok(
                new BaseResponseDto<>(HttpStatus.OK.value(), "Berhasil mengambil daftar produk", Instant.now(), response)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponseDto<Barang>> getBarangById(@PathVariable UUID id) {
        try {
            Barang barang = barangService.getBarangById(id);
            return ResponseEntity.ok(
                    new BaseResponseDto<>(HttpStatus.OK.value(), "Berhasil mengambil produk", Instant.now(), barang)
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new BaseResponseDto<>(HttpStatus.NOT_FOUND.value(), e.getMessage(), Instant.now(), null)
            );
        }
    }

    @PutMapping(value = "/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<BaseResponseDto<Barang>> updateBarang(
            @PathVariable UUID id,
            @Valid @RequestPart("data") BarangRequestDto request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        try {
            Barang updated = barangService.updateBarang(id, request, image);
            return ResponseEntity.ok(
                    new BaseResponseDto<>(HttpStatus.OK.value(), "Produk berhasil diperbarui", Instant.now(), updated)
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new BaseResponseDto<>(HttpStatus.NOT_FOUND.value(), e.getMessage(), Instant.now(), null)
            );
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new BaseResponseDto<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Gagal mengunggah foto: " + e.getMessage(), Instant.now(), null)
            );
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponseDto<Void>> deleteBarang(@PathVariable UUID id) {
        try {
            barangService.deleteBarang(id);
            return ResponseEntity.ok(
                    new BaseResponseDto<>(HttpStatus.OK.value(), "Produk berhasil dihapus", Instant.now(), null)
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new BaseResponseDto<>(HttpStatus.NOT_FOUND.value(), e.getMessage(), Instant.now(), null)
            );
        }
    }
}

