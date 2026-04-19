package io.mpruy.gor_gemilangcondet.backend_api.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.requests.StockAdjustmentRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockCardEntryResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockCardResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockItemResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockOverviewResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.Barang;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.StockMutationDirection;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.StockMutationSource;
import io.mpruy.gor_gemilangcondet.backend_api.exception.BadRequestException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.DownloadContentException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ResourceNotFoundException;
import io.mpruy.gor_gemilangcondet.backend_api.repository.BarangRepository;
import io.mpruy.gor_gemilangcondet.backend_api.service.mapper.StockMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockService {

    private final BarangRepository barangRepository;
    private final StockMutationService stockMutationService;
    private final StockMapper stockMapper;

    @Transactional(readOnly = true)
    public StockOverviewResponse getStockOverview() {
        return stockMutationService.getStockOverview();
    }

    @Transactional
    public StockItemResponse adjustStock(StockAdjustmentRequest request, UUID actorStaffId) {
        Barang barang = barangRepository.findByIdWithPessimisticLock(request.getBarangId())
                .orElseThrow(() -> new ResourceNotFoundException("Barang tidak ditemukan: " + request.getBarangId()));

        long beforeStock = barang.getStock();
        long afterStock = beforeStock;

        if (request.getDirection() == StockMutationDirection.IN) {
            afterStock = beforeStock + request.getQuantity();
        }

        if (request.getDirection() == StockMutationDirection.OUT) {
            afterStock = beforeStock - request.getQuantity();
        }

        if (afterStock < 0) {
            throw new BadRequestException(
                    "Stok akhir tidak boleh negatif. Stok saat ini: " + beforeStock + ", pengurangan: "
                            + request.getQuantity());
        }

        barang.setStock(afterStock);
        barang.setUpdatedAt(LocalDateTime.now());
        stockMapper.applySellableStatus(barang);

        stockMutationService.recordMutation(
                barang,
                request.getDirection(),
                request.getQuantity(),
                StockMutationSource.MANUAL,
                request.getReason(),
                actorStaffId,
                beforeStock,
                afterStock);

        return stockMapper.toItemResponse(barang);
    }

    @Transactional(readOnly = true)
    public StockCardResponse getStockCard(UUID barangId) {
        return stockMutationService.getStockCard(barangId);
    }

    @Transactional(readOnly = true)
    public byte[] exportStockOverviewCsv() {
        try {
            StockOverviewResponse overview = stockMutationService.getStockOverview();
            StringBuilder csv = new StringBuilder();
            csv.append("Kode,Nama,Kategori,Tipe,Harga,Stok,AmbangBatas,Status,NilaiStok\n");

            for (StockItemResponse item : overview.getItems()) {
                csv.append(stockMapper.escapeCsv(item.getKode())).append(',')
                        .append(stockMapper.escapeCsv(item.getNama())).append(',')
                        .append(stockMapper.escapeCsv(item.getKategori())).append(',')
                        .append(stockMapper.escapeCsv(item.getItemType())).append(',')
                        .append(item.getHarga()).append(',')
                        .append(item.getStok()).append(',')
                        .append(item.getAmbangBatas()).append(',')
                        .append(stockMapper.escapeCsv(item.getStatus())).append(',')
                        .append(item.getNilaiStok())
                        .append('\n');
            }

            byte[] content = csv.toString().getBytes(StandardCharsets.UTF_8);
            if (content.length == 0) {
                throw new DownloadContentException("Konten CSV stok kosong");
            }
            return content;
        } catch (DownloadContentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DownloadContentException("Ekspor data stok gagal", ex);
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportStockCardCsv(UUID barangId) {
        try {
            StockCardResponse card = stockMutationService.getStockCard(barangId);
            StringBuilder csv = new StringBuilder();
            csv.append("Waktu,Arah,Sumber,Jumlah,StokSebelum,StokSesudah,Alasan,StaffId\n");

            for (StockCardEntryResponse entry : card.getEntries()) {
                csv.append(entry.getWaktu()).append(',')
                        .append(entry.getArah()).append(',')
                        .append(entry.getSumber()).append(',')
                        .append(entry.getJumlah()).append(',')
                        .append(entry.getStokSebelum()).append(',')
                        .append(entry.getStokSesudah()).append(',')
                        .append(stockMapper.escapeCsv(entry.getAlasan())).append(',')
                        .append(entry.getStaffId() == null ? "" : entry.getStaffId())
                        .append('\n');
            }

            byte[] content = csv.toString().getBytes(StandardCharsets.UTF_8);
            if (content.length == 0) {
                throw new DownloadContentException("Konten CSV kartu stok kosong");
            }
            return content;
        } catch (DownloadContentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DownloadContentException("Ekspor kartu stok gagal", ex);
        }
    }
}