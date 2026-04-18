package io.mpruy.gor_gemilangcondet.backend_api.controller;

import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseResponseDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.requests.StockAdjustmentRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockCardResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockItemResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockOverviewResponse;
import io.mpruy.gor_gemilangcondet.backend_api.exception.BadRequestException;
import io.mpruy.gor_gemilangcondet.backend_api.security.UserDetailsImpl;
import io.mpruy.gor_gemilangcondet.backend_api.service.StockService;
import io.mpruy.gor_gemilangcondet.backend_api.util.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@CrossOrigin
@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @PreAuthorize("hasAnyAuthority('STAF_LAPANGAN', 'STAF_TOKO', 'OWNER', 'ADMIN')")
    @GetMapping("/overview")
    public ResponseEntity<BaseResponseDto<StockOverviewResponse>> getOverview() {
        StockOverviewResponse response = stockService.getStockOverview();
        return ResponseUtil.success(response, "Data stok berhasil diambil", HttpStatus.OK)
                .toBuilder().build();
    }

    @PreAuthorize("hasAnyAuthority('STAF_LAPANGAN', 'STAF_TOKO')")
    @PostMapping("/adjust")
    public ResponseEntity<BaseResponseDto<StockItemResponse>> adjustStock(
            @Valid @RequestBody StockAdjustmentRequest request) {
        UUID staffId = getAuthenticatedUserId();
        StockItemResponse response = stockService.adjustStock(request, staffId);
        return ResponseUtil.success(response, "Stok berhasil disesuaikan", HttpStatus.OK)
                .toBuilder().build();
    }

    @PreAuthorize("hasAnyAuthority('STAF_LAPANGAN', 'STAF_TOKO', 'OWNER', 'ADMIN')")
    @GetMapping("/{barangId}/card")
    public ResponseEntity<BaseResponseDto<StockCardResponse>> getStockCard(@PathVariable UUID barangId) {
        StockCardResponse response = stockService.getStockCard(barangId);
        return ResponseUtil.success(response, "Riwayat mutasi stok berhasil diambil", HttpStatus.OK)
                .toBuilder().build();
    }

    @PreAuthorize("hasAnyAuthority('STAF_LAPANGAN', 'STAF_TOKO', 'OWNER', 'ADMIN')")
    @GetMapping("/export")
    public ResponseEntity<BaseResponseDto<byte[]>> exportOverviewCsv() {
        byte[] csv = stockService.exportStockOverviewCsv();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("stock-overview.csv").build());
        return ResponseUtil.success(csv, "Data stok berhasil diekspor", HttpStatus.OK)
                .toBuilder().headers(headers).build();
    }

    @PreAuthorize("hasAnyAuthority('STAF_LAPANGAN', 'STAF_TOKO', 'OWNER', 'ADMIN')")
    @GetMapping("/{barangId}/card/export")
    public ResponseEntity<BaseResponseDto<byte[]>> exportStockCardCsv(@PathVariable UUID barangId) {
        byte[] csv = stockService.exportStockCardCsv(barangId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv"));
        headers.setContentDisposition(
                ContentDisposition.attachment().filename("stock-card-" + barangId + ".csv").build());
        return ResponseUtil.success(csv, "Data stok berhasil diekspor", HttpStatus.OK)
                .toBuilder().headers(headers).build();
    }

    private UUID getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            throw new BadRequestException("Tidak ada pengguna yang terautentikasi");
        }
        return userDetails.getUser().getId();
    }
}
