package io.mpruy.gor_gemilangcondet.backend_api.controller;

import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseResponseDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.BarangRequestDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.BarangResponseDto;
import io.mpruy.gor_gemilangcondet.backend_api.service.BarangService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

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
}

