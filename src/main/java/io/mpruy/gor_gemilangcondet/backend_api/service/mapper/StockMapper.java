package io.mpruy.gor_gemilangcondet.backend_api.service.mapper;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockCardEntryResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockCardResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockItemResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.Barang;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.StockMutation;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.alat_olahraga.AlatOlahraga;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.kantin.BarangKantin;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.kantin.BarangKantinStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.toko.BarangToko;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.toko.BarangTokoStatus;

@Service
public class StockMapper {

    private static final long ALAT_OLAHRAGA_THRESHOLD = 1L;
    private static final long TOKO_GLOBAL_THRESHOLD = 5L;

    public StockItemResponse toItemResponse(Barang barang) {
        long threshold = resolveThreshold(barang);

        return StockItemResponse.builder()
                .barangId(barang.getId())
                .kode(generateCode(barang))
                .nama(barang.getName())
                .kategori(resolveCategory(barang))
                .itemType(resolveItemType(barang))
                .harga(barang.getPrice())
                .stok(barang.getStock())
                .ambangBatas(threshold)
                .status(resolveStockStatus(barang))
                .nilaiStok(calculateStockValue(barang))
                .build();
    }

    public StockCardEntryResponse toCardEntryResponse(StockMutation mutation) {
        return StockCardEntryResponse.builder()
                .waktu(mutation.getCreatedAt())
                .arah(mutation.getDirection())
                .sumber(mutation.getSource())
                .jumlah(mutation.getQuantity())
                .stokSebelum(mutation.getBeforeStock())
                .stokSesudah(mutation.getAfterStock())
                .alasan(mutation.getReason())
                .staffId(mutation.getActorStaffId())
                .build();
    }

    public StockCardResponse toCardResponse(Barang barang, List<StockCardEntryResponse> entries) {
        return StockCardResponse.builder()
                .barangId(barang.getId())
                .kode(generateCode(barang))
                .namaBarang(barang.getName())
                .kategori(resolveCategory(barang))
                .itemType(resolveItemType(barang))
                .harga(barang.getPrice())
                .stok(barang.getStock())
                .ambangBatas(resolveThreshold(barang))
                .status(resolveStockStatus(barang))
                .nilaiStok(calculateStockValue(barang))
                .unit(barang.getUnit())
                .imageUrl(barang.getImageUrl())
                .entries(entries)
                .build();
    }

    public long resolveThreshold(Barang barang) {
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

    public String resolveCategory(Barang barang) {
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

    public String resolveItemType(Barang barang) {
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

    public String resolveStockStatus(Barang barang) {
        return barang.getStock() <= resolveThreshold(barang) ? "RENDAH" : "NORMAL";
    }

    public double calculateStockValue(Barang barang) {
        return barang.getStock() * barang.getPrice();
    }

    public void applySellableStatus(Barang barang) {
        if (barang instanceof BarangKantin kantin) {
            kantin.setStatus(barang.getStock() > 0 ? BarangKantinStatus.TERSEDIA : BarangKantinStatus.TERJUAL);
        }

        if (barang instanceof BarangToko toko) {
            toko.setStatus(barang.getStock() > 0 ? BarangTokoStatus.TERSEDIA : BarangTokoStatus.TERJUAL);
        }
    }

    public String generateCode(Barang barang) {
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

        String shortId = barang.getId().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        return prefix + "-" + shortId;
    }

    public String escapeCsv(String value) {
        if (value == null) {
            return "";
        }

        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
