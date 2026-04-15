package io.mpruy.gor_gemilangcondet.backend_api.service.mapper;

import java.util.Locale;

import org.springframework.stereotype.Service;

import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockItemResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.Barang;
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
