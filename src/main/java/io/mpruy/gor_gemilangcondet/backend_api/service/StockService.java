package io.mpruy.gor_gemilangcondet.backend_api.service;

import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.requests.StockAdjustmentRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockCardEntryResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockCardResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockItemResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockOverviewResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockSummaryResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.Barang;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.StockMutation;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.StockMutationDirection;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.StockMutationSource;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.alat_olahraga.AlatOlahraga;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.kantin.BarangKantin;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.kantin.BarangKantinStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.toko.BarangToko;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.toko.BarangTokoStatus;
import io.mpruy.gor_gemilangcondet.backend_api.exception.BadRequestException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ResourceNotFoundException;
import io.mpruy.gor_gemilangcondet.backend_api.repository.BarangRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.StockMutationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockService {

    private static final long ALAT_OLAHRAGA_THRESHOLD = 1L;
    private static final long TOKO_GLOBAL_THRESHOLD = 5L;

    private final BarangRepository barangRepository;
    private final StockMutationRepository stockMutationRepository;

    @Transactional(readOnly = true)
    public StockOverviewResponse getStockOverview() {
        List<StockItemResponse> items = barangRepository.findAllSellable().stream()
                .sorted(Comparator.comparing(Barang::getName))
                .map(this::toItemResponse)
                .toList();

        long lowStock = items.stream().filter(i -> "RENDAH".equals(i.getStatus())).count();
        double totalValue = items.stream().mapToDouble(StockItemResponse::getNilaiStok).sum();

        StockSummaryResponse summary = StockSummaryResponse.builder()
                .totalProduk(items.size())
                .stokRendah(lowStock)
                .nilaiStok(totalValue)
                .build();

        return StockOverviewResponse.builder()
                .summary(summary)
                .items(items)
                .build();
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
        applySellableStatus(barang);

        recordMutation(
                barang,
                request.getDirection(),
                request.getQuantity(),
                StockMutationSource.MANUAL,
                request.getReason(),
                actorStaffId,
                beforeStock,
                afterStock);

        return toItemResponse(barang);
    }

    @Transactional(readOnly = true)
    public StockCardResponse getStockCard(UUID barangId) {
        Barang barang = barangRepository.findById(barangId)
                .orElseThrow(() -> new ResourceNotFoundException("Barang tidak ditemukan: " + barangId));

        List<StockCardEntryResponse> entries = stockMutationRepository.findByBarangIdOrderByCreatedAtDesc(barangId)
                .stream()
                .map(m -> StockCardEntryResponse.builder()
                        .waktu(m.getCreatedAt())
                        .arah(m.getDirection())
                        .sumber(m.getSource())
                        .jumlah(m.getQuantity())
                        .stokSebelum(m.getBeforeStock())
                        .stokSesudah(m.getAfterStock())
                        .alasan(m.getReason())
                        .staffId(m.getActorStaffId())
                        .build())
                .toList();

        return StockCardResponse.builder()
                .barangId(barang.getId())
                .namaBarang(barang.getName())
                .entries(entries)
                .build();
    }

    @Transactional(readOnly = true)
    public byte[] exportStockOverviewCsv() {
        StockOverviewResponse overview = getStockOverview();
        StringBuilder csv = new StringBuilder();
        csv.append("Kode,Nama,Kategori,Tipe,Harga,Stok,AmbangBatas,Status,NilaiStok\n");

        for (StockItemResponse item : overview.getItems()) {
            csv.append(escapeCsv(item.getKode())).append(',')
                    .append(escapeCsv(item.getNama())).append(',')
                    .append(escapeCsv(item.getKategori())).append(',')
                    .append(escapeCsv(item.getItemType())).append(',')
                    .append(item.getHarga()).append(',')
                    .append(item.getStok()).append(',')
                    .append(item.getAmbangBatas()).append(',')
                    .append(escapeCsv(item.getStatus())).append(',')
                    .append(item.getNilaiStok())
                    .append('\n');
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] exportStockCardCsv(UUID barangId) {
        StockCardResponse card = getStockCard(barangId);
        StringBuilder csv = new StringBuilder();
        csv.append("Waktu,Arah,Sumber,Jumlah,StokSebelum,StokSesudah,Alasan,StaffId\n");

        for (StockCardEntryResponse entry : card.getEntries()) {
            csv.append(entry.getWaktu()).append(',')
                    .append(entry.getArah()).append(',')
                    .append(entry.getSumber()).append(',')
                    .append(entry.getJumlah()).append(',')
                    .append(entry.getStokSebelum()).append(',')
                    .append(entry.getStokSesudah()).append(',')
                    .append(escapeCsv(entry.getAlasan())).append(',')
                    .append(entry.getStaffId() == null ? "" : entry.getStaffId())
                    .append('\n');
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public void recordMutation(
            Barang barang,
            StockMutationDirection direction,
            long quantity,
            StockMutationSource source,
            String reason,
            UUID actorStaffId,
            long beforeStock,
            long afterStock) {
        StockMutation mutation = StockMutation.builder()
                .barang(barang)
                .direction(direction)
                .quantity(quantity)
                .beforeStock(beforeStock)
                .afterStock(afterStock)
                .source(source)
                .reason(reason)
                .actorStaffId(actorStaffId)
                .createdAt(LocalDateTime.now())
                .build();

        stockMutationRepository.save(mutation);
    }

    private StockItemResponse toItemResponse(Barang barang) {
        long threshold = resolveThreshold(barang);
        String status = barang.getStock() <= threshold ? "RENDAH" : "NORMAL";

        return StockItemResponse.builder()
                .barangId(barang.getId())
                .kode(generateCode(barang))
                .nama(barang.getName())
                .kategori(resolveCategory(barang))
                .itemType(resolveItemType(barang))
                .harga(barang.getPrice())
                .stok(barang.getStock())
                .ambangBatas(threshold)
                .status(status)
                .nilaiStok(barang.getStock() * barang.getPrice())
                .build();
    }

    private long resolveThreshold(Barang barang) {
        if (barang instanceof AlatOlahraga) {
            return ALAT_OLAHRAGA_THRESHOLD;
        }

        if (barang instanceof BarangKantin kantin) {
            return Math.max(kantin.getReorderThreshold(), 0L);
        }

        if (barang instanceof BarangToko) {
            return TOKO_GLOBAL_THRESHOLD;
        }

        return 0L;
    }

    private String resolveCategory(Barang barang) {
        if (barang instanceof AlatOlahraga) {
            return "ALAT_OLAHRAGA";
        }

        if (barang instanceof BarangKantin) {
            return "BARANG_KANTIN";
        }

        if (barang instanceof BarangToko) {
            return "BARANG_TOKO";
        }

        return "UNKNOWN";
    }

    private String resolveItemType(Barang barang) {
        if (barang instanceof AlatOlahraga alat) {
            return alat.getType().name();
        }

        if (barang instanceof BarangKantin kantin) {
            return kantin.getType().name();
        }

        if (barang instanceof BarangToko toko) {
            return toko.getType().name();
        }

        return "-";
    }

    private void applySellableStatus(Barang barang) {
        if (barang instanceof BarangKantin kantin) {
            kantin.setStatus(barang.getStock() > 0 ? BarangKantinStatus.TERSEDIA : BarangKantinStatus.TERJUAL);
        }

        if (barang instanceof BarangToko toko) {
            toko.setStatus(barang.getStock() > 0 ? BarangTokoStatus.TERSEDIA : BarangTokoStatus.TERJUAL);
        }
    }

    private String generateCode(Barang barang) {
        String prefix = "BRG";
        if (barang instanceof AlatOlahraga) {
            prefix = "ALT";
        } else if (barang instanceof BarangKantin) {
            prefix = "KNT";
        } else if (barang instanceof BarangToko) {
            prefix = "TOK";
        }

        if (barang.getId() == null) {
            return prefix + "-NA";
        }

        String shortId = barang.getId().toString().replace("-", "").substring(0, 8).toUpperCase();
        return prefix + "-" + shortId;
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }

        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
