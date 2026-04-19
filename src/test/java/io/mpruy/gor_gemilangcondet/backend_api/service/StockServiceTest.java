package io.mpruy.gor_gemilangcondet.backend_api.service;

import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.requests.StockAdjustmentRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockCardEntryResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockCardResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockItemResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockOverviewResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockSummaryResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.StockMutationDirection;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.StockMutationSource;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.kantin.BarangKantin;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.kantin.BarangKantinStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.kantin.BarangKantinType;
import io.mpruy.gor_gemilangcondet.backend_api.exception.BadRequestException;
import io.mpruy.gor_gemilangcondet.backend_api.repository.BarangRepository;
import io.mpruy.gor_gemilangcondet.backend_api.service.mapper.StockMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private BarangRepository barangRepository;

    @Mock
    private StockMutationService stockMutationService;

    @Spy
    private StockMapper stockMapper;

    @InjectMocks
    private StockService stockService;

    @Test
    void getStockOverview_ShouldDelegateToMutationService() {
        StockOverviewResponse expected = StockOverviewResponse.builder()
                .summary(StockSummaryResponse.builder().totalProduk(1).stokRendah(0).nilaiStok(15000).build())
                .items(List.of())
                .build();

        when(stockMutationService.getStockOverview()).thenReturn(expected);

        StockOverviewResponse result = stockService.getStockOverview();

        assertEquals(expected, result);
        verify(stockMutationService).getStockOverview();
    }

    @Test
    void adjustStock_ShouldIncreaseStockAndRecordMutation() {
        UUID barangId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        BarangKantin kantin = BarangKantin.builder()
                .id(barangId)
                .name("Nasi Goreng")
                .type(BarangKantinType.MAKANAN_BERAT)
                .status(BarangKantinStatus.TERSEDIA)
                .reorderThreshold(3)
                .stock(5)
                .price(15000)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        StockAdjustmentRequest request = new StockAdjustmentRequest();
        request.setBarangId(barangId);
        request.setDirection(StockMutationDirection.IN);
        request.setQuantity(2);
        request.setReason("Restock");

        when(barangRepository.findByIdWithPessimisticLock(barangId)).thenReturn(Optional.of(kantin));

        StockItemResponse result = stockService.adjustStock(request, staffId);

        assertEquals(7, kantin.getStock());
        assertEquals(7, result.getStok());
        verify(stockMutationService).recordMutation(
                kantin,
                StockMutationDirection.IN,
                2,
                StockMutationSource.MANUAL,
                "Restock",
                staffId,
                5,
                7);
    }

    @Test
    void adjustStock_ShouldRejectNegativeFinalStock() {
        UUID barangId = UUID.randomUUID();
        BarangKantin kantin = BarangKantin.builder()
                .id(barangId)
                .name("Nasi Goreng")
                .type(BarangKantinType.MAKANAN_BERAT)
                .status(BarangKantinStatus.TERSEDIA)
                .reorderThreshold(10)
                .stock(5)
                .price(15000)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        StockAdjustmentRequest request = new StockAdjustmentRequest();
        request.setBarangId(barangId);
        request.setDirection(StockMutationDirection.OUT);
        request.setQuantity(6);
        request.setReason("Penyesuaian audit");

        when(barangRepository.findByIdWithPessimisticLock(barangId)).thenReturn(Optional.of(kantin));

        assertThrows(BadRequestException.class, () -> stockService.adjustStock(request, UUID.randomUUID()));
        verify(stockMutationService, never()).recordMutation(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void adjustStock_ShouldLogMutationAndUpdateSellableStatus() {
        UUID barangId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        BarangKantin kantin = BarangKantin.builder()
                .id(barangId)
                .name("Nasi Goreng")
                .type(BarangKantinType.MAKANAN_BERAT)
                .status(BarangKantinStatus.TERSEDIA)
                .reorderThreshold(3)
                .stock(5)
                .price(15000)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        StockAdjustmentRequest request = new StockAdjustmentRequest();
        request.setBarangId(barangId);
        request.setDirection(StockMutationDirection.OUT);
        request.setQuantity(5);
        request.setReason("Barang rusak");

        when(barangRepository.findByIdWithPessimisticLock(barangId)).thenReturn(Optional.of(kantin));

        StockItemResponse result = stockService.adjustStock(request, staffId);

        assertEquals(0, kantin.getStock());
        assertEquals(BarangKantinStatus.TERJUAL, kantin.getStatus());
        assertEquals("RENDAH", result.getStatus());
        verify(stockMutationService).recordMutation(
                kantin,
                StockMutationDirection.OUT,
                5,
                StockMutationSource.MANUAL,
                "Barang rusak",
                staffId,
                5,
                0);
    }

    @Test
    void getStockCard_ShouldDelegateToMutationService() {
        UUID barangId = UUID.randomUUID();
        StockCardResponse expected = StockCardResponse.builder()
                .barangId(barangId)
                .namaBarang("Nasi Goreng")
                .entries(List.of())
                .build();

        when(stockMutationService.getStockCard(barangId)).thenReturn(expected);

        StockCardResponse result = stockService.getStockCard(barangId);

        assertEquals(expected, result);
        verify(stockMutationService).getStockCard(barangId);
    }

    @Test
    void exportStockOverviewCsv_ShouldGenerateCsvRows() {
        StockItemResponse item = StockItemResponse.builder()
                .barangId(UUID.randomUUID())
                .kode("KNT-12345678")
                .nama("Nasi Goreng")
                .kategori("BARANG_KANTIN")
                .itemType("MAKANAN_BERAT")
                .harga(15000)
                .stok(5)
                .ambangBatas(3)
                .status("NORMAL")
                .nilaiStok(75000)
                .build();

        when(stockMutationService.getStockOverview()).thenReturn(
                StockOverviewResponse.builder()
                        .summary(StockSummaryResponse.builder().totalProduk(1).stokRendah(0).nilaiStok(75000).build())
                        .items(List.of(item))
                        .build());

        byte[] bytes = stockService.exportStockOverviewCsv();
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertTrue(csv.contains("Kode,Nama,Kategori,Tipe,Harga,Stok,AmbangBatas,Status,NilaiStok"));
        assertTrue(csv.contains("\"Nasi Goreng\""));
        assertTrue(csv.contains("75000.0"));
    }

    @Test
    void exportStockCardCsv_ShouldGenerateCsvRows() {
        UUID barangId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        StockCardEntryResponse entry = StockCardEntryResponse.builder()
                .waktu(now)
                .arah(StockMutationDirection.OUT)
                .sumber(StockMutationSource.MANUAL)
                .jumlah(2)
                .stokSebelum(5)
                .stokSesudah(3)
                .alasan("Barang rusak")
                .staffId(staffId)
                .build();

        when(stockMutationService.getStockCard(barangId)).thenReturn(
                StockCardResponse.builder()
                        .barangId(barangId)
                        .namaBarang("Nasi Goreng")
                        .entries(List.of(entry))
                        .build());

        byte[] bytes = stockService.exportStockCardCsv(barangId);
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertTrue(csv.contains("Waktu,Arah,Sumber,Jumlah,StokSebelum,StokSesudah,Alasan,StaffId"));
        assertTrue(csv.contains("Barang rusak"));
        assertTrue(csv.contains(staffId.toString()));
    }

    @Test
    void exportStockCardCsv_ShouldReturnEmptyStaffIdWhenNull() {
        UUID barangId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        StockCardEntryResponse entry = StockCardEntryResponse.builder()
                .waktu(now)
                .arah(StockMutationDirection.IN)
                .sumber(StockMutationSource.MANUAL)
                .jumlah(2)
                .stokSebelum(1)
                .stokSesudah(3)
                .alasan("Restock")
                .staffId(null)
                .build();

        when(stockMutationService.getStockCard(barangId)).thenReturn(
                StockCardResponse.builder()
                        .barangId(barangId)
                        .namaBarang("Nasi Goreng")
                        .entries(List.of(entry))
                        .build());

        byte[] bytes = stockService.exportStockCardCsv(barangId);
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertTrue(csv.contains("Restock"));
        assertTrue(csv.contains(",\n"));
    }
}
