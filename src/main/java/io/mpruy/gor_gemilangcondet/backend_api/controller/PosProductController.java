package io.mpruy.gor_gemilangcondet.backend_api.controller;

import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseRequestDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseResponseDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.pos.requests.CreatePosProductRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.pos.requests.UpdatePosProductRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.pos.responses.PosProductResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.pos.PosProductCategory;
import io.mpruy.gor_gemilangcondet.backend_api.service.PosProductService;
import io.mpruy.gor_gemilangcondet.backend_api.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin
@RestController
@RequestMapping("/pos/products")
@RequiredArgsConstructor
public class PosProductController {

    private final PosProductService posProductService;

    @PreAuthorize("hasAnyAuthority('ADMIN', 'OWNER', 'STAF_TOKO', 'STAF_LAPANGAN')")
    @GetMapping
    public ResponseEntity<BaseResponseDto<List<PosProductResponse>>> getProducts(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(required = false) PosProductCategory category) {
        List<PosProductResponse> products = posProductService.getProducts(query, category);
        return ResponseUtil.success(products, "Data produk berhasil diambil", HttpStatus.OK)
                .toBuilder().build();
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'OWNER', 'STAF_TOKO', 'STAF_LAPANGAN')")
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponseDto<PosProductResponse>> getProductById(@PathVariable UUID id) {
        PosProductResponse product = posProductService.getProductById(id);
        return ResponseUtil.success(product, "Detail produk berhasil diambil", HttpStatus.OK)
                .toBuilder().build();
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'OWNER', 'STAF_TOKO', 'STAF_LAPANGAN')")
    @PostMapping
    public ResponseEntity<BaseResponseDto<PosProductResponse>> createProduct(
            @Validated @RequestBody BaseRequestDto<CreatePosProductRequest> request) {
        PosProductResponse created = posProductService.createProduct(request.getData());
        return ResponseUtil.success(created, "Produk berhasil ditambahkan", HttpStatus.CREATED)
                .toBuilder().build();
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'OWNER', 'STAF_TOKO', 'STAF_LAPANGAN')")
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponseDto<PosProductResponse>> updateProduct(
            @PathVariable UUID id,
            @Validated @RequestBody BaseRequestDto<UpdatePosProductRequest> request) {
        PosProductResponse updated = posProductService.updateProduct(id, request.getData());
        return ResponseUtil.success(updated, "Produk berhasil diperbarui", HttpStatus.OK)
                .toBuilder().build();
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'OWNER', 'STAF_TOKO', 'STAF_LAPANGAN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponseDto<Object>> deleteProduct(@PathVariable UUID id) {
        posProductService.deleteProduct(id);
        return ResponseUtil.success(null, "Produk berhasil dihapus", HttpStatus.OK)
                .toBuilder().build();
    }
}
