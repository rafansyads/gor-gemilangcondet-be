package io.mpruy.gor_gemilangcondet.backend_api.controller;

import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseResponseDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.transaksi.requests.CheckoutRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.transaksi.responses.TransaksiResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.Barang;
import io.mpruy.gor_gemilangcondet.backend_api.exception.BadRequestException;
import io.mpruy.gor_gemilangcondet.backend_api.repository.BarangRepository;
import io.mpruy.gor_gemilangcondet.backend_api.security.UserDetailsImpl;
import io.mpruy.gor_gemilangcondet.backend_api.service.TransaksiService;
import io.mpruy.gor_gemilangcondet.backend_api.util.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin
@RestController
@RequestMapping("/transaksi")
@RequiredArgsConstructor
public class TransaksiController {

    private final TransaksiService transaksiService;
    private final BarangRepository barangRepository;

    @PreAuthorize("hasAnyAuthority('STAF_LAPANGAN', 'STAF_TOKO', 'OWNER', 'ADMIN')")
    @GetMapping("/barang")
    public ResponseEntity<BaseResponseDto<List<Barang>>> getAllBarang() {
        List<Barang> list = barangRepository.findAllSellable();
        return ResponseUtil.success(list, "Data barang berhasil diambil", HttpStatus.OK)
                .toBuilder().build();
    }

    @PreAuthorize("hasAnyAuthority('STAF_LAPANGAN', 'STAF_TOKO')")
    @PostMapping("/checkout")
    public ResponseEntity<BaseResponseDto<TransaksiResponse>> checkout(
            @Valid @RequestBody CheckoutRequest request) {
        UUID staffId = getAuthenticatedUserId();
        TransaksiResponse response = transaksiService.checkout(request, staffId);
        return ResponseUtil.success(response, "Transaksi berhasil dicatat", HttpStatus.CREATED)
                .toBuilder().build();
    }

    @PreAuthorize("hasAnyAuthority('STAF_LAPANGAN', 'STAF_TOKO', 'OWNER', 'ADMIN')")
    @GetMapping
    public ResponseEntity<BaseResponseDto<List<TransaksiResponse>>> getAllTransaksi() {
        List<TransaksiResponse> list = transaksiService.getAllTransaksi();
        return ResponseUtil.success(list, "Data transaksi berhasil diambil", HttpStatus.OK)
                .toBuilder().build();
    }

    @PreAuthorize("hasAnyAuthority('STAF_LAPANGAN', 'STAF_TOKO', 'OWNER', 'ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponseDto<TransaksiResponse>> getTransaksiById(
            @PathVariable UUID id) {
        TransaksiResponse response = transaksiService.getTransaksiById(id);
        return ResponseUtil.success(response, "Detail transaksi berhasil diambil", HttpStatus.OK)
                .toBuilder().build();
    }

    private UUID getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            throw new BadRequestException("Tidak ada pengguna yang terautentikasi");
        }
        return userDetails.getUser().getId();
    }
}
