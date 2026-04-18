package io.mpruy.gor_gemilangcondet.backend_api.service;

import io.mpruy.gor_gemilangcondet.backend_api.dto.pos.requests.CreatePosProductRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.pos.requests.UpdatePosProductRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.pos.responses.PosProductResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.pos.PosProduct;
import io.mpruy.gor_gemilangcondet.backend_api.entities.pos.PosProductCategory;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ConflictException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ResourceNotFoundException;
import io.mpruy.gor_gemilangcondet.backend_api.repository.PosProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PosProductService {

    private final PosProductRepository posProductRepository;

    @Transactional(readOnly = true)
    public List<PosProductResponse> getProducts(String query, PosProductCategory category) {
        String normalizedQuery = normalize(query);
        return posProductRepository.search(normalizedQuery, category)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PosProductResponse getProductById(UUID id) {
        PosProduct product = posProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produk tidak ditemukan: " + id));
        return toResponse(product);
    }

    @Transactional
    public PosProductResponse createProduct(CreatePosProductRequest request) {
        String normalizedSku = normalize(request.getSku());

        if (normalizedSku != null && posProductRepository.existsBySkuIgnoreCase(normalizedSku)) {
            throw new ConflictException("SKU produk sudah digunakan: " + normalizedSku);
        }

        PosProduct created = posProductRepository.save(PosProduct.builder()
                .name(normalize(request.getName()))
                .sku(normalizedSku)
                .price(request.getPrice())
                .stock(request.getStock())
                .category(request.getCategory())
                .imageUrl(normalizeImageUrl(request.getImageUrl()))
                .build());

        return toResponse(created);
    }

    @Transactional
    public PosProductResponse updateProduct(UUID id, UpdatePosProductRequest request) {
        PosProduct existing = posProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produk tidak ditemukan: " + id));

        String normalizedSku = normalize(request.getSku());
        boolean skuChanged = normalizedSku != null && !normalizedSku.equalsIgnoreCase(existing.getSku());

        if (skuChanged && posProductRepository.existsBySkuIgnoreCase(normalizedSku)) {
            throw new ConflictException("SKU produk sudah digunakan: " + normalizedSku);
        }

        existing.setName(normalize(request.getName()));
        existing.setSku(normalizedSku);
        existing.setPrice(request.getPrice());
        existing.setStock(request.getStock());
        existing.setCategory(request.getCategory());
        existing.setImageUrl(normalizeImageUrl(request.getImageUrl()));

        PosProduct updated = posProductRepository.save(existing);
        return toResponse(updated);
    }

    @Transactional
    public void deleteProduct(UUID id) {
        PosProduct existing = posProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produk tidak ditemukan: " + id));
        posProductRepository.delete(existing);
    }

    private PosProductResponse toResponse(PosProduct product) {
        return PosProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .price(product.getPrice())
                .stock(product.getStock())
                .category(product.getCategory())
                .imageUrl(product.getImageUrl())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeImageUrl(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized;
    }
}
