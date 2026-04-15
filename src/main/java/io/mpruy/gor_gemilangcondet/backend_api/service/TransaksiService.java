package io.mpruy.gor_gemilangcondet.backend_api.service;

import io.mpruy.gor_gemilangcondet.backend_api.dto.transaksi.requests.CartItemRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.transaksi.requests.CheckoutRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.transaksi.responses.TransaksiDetailResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.transaksi.responses.TransaksiResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.Barang;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.StockMutationDirection;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.StockMutationSource;
import io.mpruy.gor_gemilangcondet.backend_api.entities.transaksi.Transaksi;
import io.mpruy.gor_gemilangcondet.backend_api.entities.transaksi.TransaksiDetail;
import io.mpruy.gor_gemilangcondet.backend_api.entities.transaksi.TransaksiStatus;
import io.mpruy.gor_gemilangcondet.backend_api.exception.BadRequestException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ResourceNotFoundException;
import io.mpruy.gor_gemilangcondet.backend_api.repository.BarangRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.TransaksiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransaksiService {

    private final TransaksiRepository transaksiRepository;
    private final BarangRepository barangRepository;
    private final StockService stockService;

    @Transactional
    public TransaksiResponse checkout(CheckoutRequest request, UUID staffId) {
        Transaksi transaksi = Transaksi.builder()
                .staffId(staffId)
                .paymentMethod(request.getPaymentMethod())
                .status(TransaksiStatus.SELESAI)
                .diskon(request.getDiskon())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        double subtotal = 0;

        for (CartItemRequest item : request.getItems()) {
            Barang barang = barangRepository.findByIdWithPessimisticLock(item.getBarangId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Barang tidak ditemukan: " + item.getBarangId()));

            long beforeStock = barang.getStock();

            if (barang.getStock() < item.getKuantitas()) {
                throw new BadRequestException(
                        "Stok " + barang.getName() + " tidak mencukupi. Tersedia: "
                                + barang.getStock() + ", diminta: " + item.getKuantitas());
            }

            barang.setStock(barang.getStock() - item.getKuantitas());
            barang.setUpdatedAt(LocalDateTime.now());

            stockService.recordMutation(
                    barang,
                    StockMutationDirection.OUT,
                    item.getKuantitas(),
                    StockMutationSource.POS,
                    "Checkout transaksi",
                    staffId,
                    beforeStock,
                    barang.getStock());

            double lineSubtotal = barang.getPrice() * item.getKuantitas();
            subtotal += lineSubtotal;

            TransaksiDetail detail = TransaksiDetail.builder()
                    .transaksi(transaksi)
                    .barang(barang)
                    .hargaSatuan(barang.getPrice())
                    .kuantitas(item.getKuantitas())
                    .subtotal(lineSubtotal)
                    .build();

            transaksi.getDetails().add(detail);
        }

        transaksi.setSubtotal(subtotal);
        transaksi.setGrandTotal(subtotal - request.getDiskon());

        if (transaksi.getGrandTotal() < 0) {
            throw new BadRequestException("Diskon tidak boleh melebihi subtotal");
        }

        transaksi = transaksiRepository.save(transaksi);
        return toResponse(transaksi);
    }

    @Transactional(readOnly = true)
    public List<TransaksiResponse> getAllTransaksi() {
        return transaksiRepository.findAllWithDetails().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransaksiResponse getTransaksiById(UUID id) {
        Transaksi transaksi = transaksiRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaksi tidak ditemukan: " + id));
        return toResponse(transaksi);
    }

    private TransaksiResponse toResponse(Transaksi t) {
        List<TransaksiDetailResponse> items = t.getDetails().stream()
                .map(d -> TransaksiDetailResponse.builder()
                        .id(d.getId())
                        .barangId(d.getBarang().getId())
                        .barangName(d.getBarang().getName())
                        .hargaSatuan(d.getHargaSatuan())
                        .kuantitas(d.getKuantitas())
                        .subtotal(d.getSubtotal())
                        .build())
                .toList();

        return TransaksiResponse.builder()
                .id(t.getId())
                .staffId(t.getStaffId())
                .paymentMethod(t.getPaymentMethod())
                .status(t.getStatus())
                .subtotal(t.getSubtotal())
                .diskon(t.getDiskon())
                .grandTotal(t.getGrandTotal())
                .items(items)
                .createdAt(t.getCreatedAt())
                .build();
    }
}
