package io.mpruy.gor_gemilangcondet.backend_api.service;

import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.requests.StockAdjustmentRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockCardResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockOverviewResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.StockMutation;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.StockMutationDirection;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.StockMutationSource;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.kantin.BarangKantin;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.kantin.BarangKantinStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.kantin.BarangKantinType;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.toko.BarangToko;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.toko.BarangTokoStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.toko.BarangTokoType;
import io.mpruy.gor_gemilangcondet.backend_api.exception.BadRequestException;
import io.mpruy.gor_gemilangcondet.backend_api.repository.BarangRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.StockMutationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private BarangRepository barangRepository;

    @Mock
    private StockMutationRepository stockMutationRepository;

    @InjectMocks
    private StockService stockService;

    @Test
    void getStockOverview_ShouldComputeSummaryAndLowStock() {
        BarangKantin kantin = BarangKantin.builder()
                .id(UUID.randomUUID())
                .name("Nasi Goreng")
                .type(BarangKantinType.MAKANAN_BERAT)
                .status(BarangKantinStatus.TERSEDIA)
                .reorderThreshold(10)
                .stock(5)
                .price(15000)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        BarangToko toko = BarangToko.builder()
                .id(UUID.randomUUID())
                .name("Raket Yonex")
                .type(BarangTokoType.ALAT_OLAHRAGA)
                .status(BarangTokoStatus.TERSEDIA)
                .stock(8)
                .price(450000)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(barangRepository.findAllSellable()).thenReturn(List.of(kantin, toko));

        StockOverviewResponse response = stockService.getStockOverview();

        assertEquals(2, response.getSummary().getTotalProduk());
        assertEquals(1, response.getSummary().getStokRendah());
        assertEquals(5 * 15000 + 8 * 450000, response.getSummary().getNilaiStok());
        assertTrue(response.getItems().stream().anyMatch(i -> "RENDAH".equals(i.getStatus())));
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
        verify(stockMutationRepository, never()).save(any(StockMutation.class));
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

        stockService.adjustStock(request, staffId);

        assertEquals(0, kantin.getStock());
        assertEquals(BarangKantinStatus.TERJUAL, kantin.getStatus());

        ArgumentCaptor<StockMutation> mutationCaptor = ArgumentCaptor.forClass(StockMutation.class);
        verify(stockMutationRepository).save(mutationCaptor.capture());
        StockMutation mutation = mutationCaptor.getValue();

        assertEquals(StockMutationDirection.OUT, mutation.getDirection());
        assertEquals(StockMutationSource.MANUAL, mutation.getSource());
        assertEquals(5, mutation.getQuantity());
        assertEquals(5, mutation.getBeforeStock());
        assertEquals(0, mutation.getAfterStock());
        assertEquals(staffId, mutation.getActorStaffId());
    }

    @Test
    void getStockCard_ShouldReturnMappedEntries() {
        UUID barangId = UUID.randomUUID();
        BarangToko toko = BarangToko.builder()
                .id(barangId)
                .name("Raket Yonex")
                .type(BarangTokoType.ALAT_OLAHRAGA)
                .status(BarangTokoStatus.TERSEDIA)
                .stock(8)
                .price(450000)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        StockMutation mutation = StockMutation.builder()
                .id(UUID.randomUUID())
                .barang(toko)
                .direction(StockMutationDirection.OUT)
                .quantity(1)
                .beforeStock(9)
                .afterStock(8)
                .source(StockMutationSource.POS)
                .reason("Checkout transaksi")
                .actorStaffId(UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .build();

        when(barangRepository.findById(barangId)).thenReturn(Optional.of(toko));
        when(stockMutationRepository.findByBarangIdOrderByCreatedAtDesc(barangId)).thenReturn(List.of(mutation));

        StockCardResponse response = stockService.getStockCard(barangId);

        assertEquals(barangId, response.getBarangId());
        assertEquals("Raket Yonex", response.getNamaBarang());
        assertEquals(1, response.getEntries().size());
        assertEquals(StockMutationDirection.OUT, response.getEntries().getFirst().getArah());
    }
}
