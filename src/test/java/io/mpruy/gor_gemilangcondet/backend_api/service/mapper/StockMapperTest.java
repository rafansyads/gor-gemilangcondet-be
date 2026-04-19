package io.mpruy.gor_gemilangcondet.backend_api.service.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockCardEntryResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockCardResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockItemResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.Barang;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.StockMutation;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.StockMutationDirection;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.StockMutationSource;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.alat_olahraga.AlatOlahraga;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.alat_olahraga.AlatOlahragaStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.alat_olahraga.AlatOlahragaType;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.kantin.BarangKantin;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.kantin.BarangKantinStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.kantin.BarangKantinType;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.toko.BarangToko;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.toko.BarangTokoStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.toko.BarangTokoType;

class StockMapperTest {

    private final StockMapper stockMapper = new StockMapper();

    @Test
    void toItemResponse_ShouldMapKantinAndResolveLowStockStatus() {
        BarangKantin kantin = BarangKantin.builder()
                .id(UUID.randomUUID())
                .name("Nasi Goreng")
                .type(BarangKantinType.MAKANAN_BERAT)
                .status(BarangKantinStatus.TERSEDIA)
                .reorderThreshold(10)
                .stock(5)
                .price(15000)
                .build();

        StockItemResponse response = stockMapper.toItemResponse(kantin);

        assertEquals("BARANG_KANTIN", response.getKategori());
        assertEquals("MAKANAN_BERAT", response.getItemType());
        assertEquals(10, response.getAmbangBatas());
        assertEquals("RENDAH", response.getStatus());
        assertEquals(75000, response.getNilaiStok());
    }

    @Test
    void resolveMethods_ShouldHandleEquipmentAndFallbackUnknown() {
        AlatOlahraga alat = AlatOlahraga.builder()
                .id(UUID.randomUUID())
                .name("Raket")
                .type(AlatOlahragaType.RAKET)
                .status(AlatOlahragaStatus.TERSEDIA)
                .stock(2)
                .price(90000)
                .build();

        assertEquals(1, stockMapper.resolveThreshold(alat));
        assertEquals("ALAT_OLAHRAGA", stockMapper.resolveCategory(alat));
        assertEquals("RAKET", stockMapper.resolveItemType(alat));
        assertEquals("NORMAL", stockMapper.resolveStockStatus(alat));
        assertEquals(180000, stockMapper.calculateStockValue(alat));

        Barang unknown = mock(Barang.class);
        when(unknown.getStock()).thenReturn(0L);
        when(unknown.getPrice()).thenReturn(0d);
        assertEquals(0, stockMapper.resolveThreshold(unknown));
        assertEquals("UNKNOWN", stockMapper.resolveCategory(unknown));
        assertEquals("-", stockMapper.resolveItemType(unknown));
    }

    @Test
    void applySellableStatus_ShouldSetKantinAndTokoStatusBasedOnStock() {
        BarangKantin kantin = BarangKantin.builder()
                .id(UUID.randomUUID())
                .name("Air Mineral")
                .type(BarangKantinType.MINUMAN)
                .status(BarangKantinStatus.TERSEDIA)
                .reorderThreshold(2)
                .stock(0)
                .price(5000)
                .build();

        BarangToko toko = BarangToko.builder()
                .id(UUID.randomUUID())
                .name("Jersey")
                .type(BarangTokoType.PAKAIAN)
                .status(BarangTokoStatus.TERSEDIA)
                .stock(3)
                .price(100000)
                .build();

        stockMapper.applySellableStatus(kantin);
        stockMapper.applySellableStatus(toko);

        assertEquals(BarangKantinStatus.TERJUAL, kantin.getStatus());
        assertEquals(BarangTokoStatus.TERSEDIA, toko.getStatus());
    }

    @Test
    void generateCode_ShouldUsePrefixAndFallbackWhenIdIsNull() {
        BarangKantin kantin = BarangKantin.builder()
                .id(UUID.fromString("01234567-89ab-cdef-0123-456789abcdef"))
                .name("Roti")
                .type(BarangKantinType.MAKANAN_RINGAN)
                .status(BarangKantinStatus.TERSEDIA)
                .reorderThreshold(1)
                .stock(4)
                .price(8000)
                .build();

        BarangToko tokoNoId = BarangToko.builder()
                .id(null)
                .name("Bola")
                .type(BarangTokoType.ALAT_OLAHRAGA)
                .status(BarangTokoStatus.TERSEDIA)
                .stock(4)
                .price(8000)
                .build();

        assertEquals("KNT-01234567", stockMapper.generateCode(kantin));
        assertEquals("TOK-NA", stockMapper.generateCode(tokoNoId));
    }

    @Test
    void escapeCsv_ShouldQuoteAndEscapeDoubleQuoteCharacters() {
        assertEquals("", stockMapper.escapeCsv(null));
        assertEquals("\"Produk\"", stockMapper.escapeCsv("Produk"));
        assertEquals("\"Kata \"\"quote\"\"\"", stockMapper.escapeCsv("Kata \"quote\""));
    }

    @Test
    void toCardMappers_ShouldProvideExtendedFrontendFields() {
        UUID barangId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        BarangToko toko = BarangToko.builder()
                .id(barangId)
                .name("Sepatu")
                .type(BarangTokoType.PAKAIAN)
                .status(BarangTokoStatus.TERSEDIA)
                .stock(4)
                .price(250000)
                .unit("pasang")
                .imageUrl("https://img.example/sepatu.png")
                .build();

        StockMutation mutation = StockMutation.builder()
                .id(UUID.randomUUID())
                .barang(toko)
                .direction(StockMutationDirection.OUT)
                .quantity(1)
                .beforeStock(5)
                .afterStock(4)
                .source(StockMutationSource.MANUAL)
                .reason("Display")
                .actorStaffId(staffId)
                .createdAt(now)
                .build();

        StockCardEntryResponse entry = stockMapper.toCardEntryResponse(mutation);
        StockCardResponse response = stockMapper.toCardResponse(toko, List.of(entry));

        assertEquals(staffId, entry.getStaffId());
        assertEquals("TOK-" + barangId.toString().replace("-", "").substring(0, 8).toUpperCase(), response.getKode());
        assertEquals("BARANG_TOKO", response.getKategori());
        assertEquals("PAKAIAN", response.getItemType());
        assertEquals(5, response.getAmbangBatas());
        assertEquals("RENDAH", response.getStatus());
        assertEquals("pasang", response.getUnit());
        assertNotNull(response.getEntries());
        assertTrue(response.getEntries().size() == 1);
    }
}
