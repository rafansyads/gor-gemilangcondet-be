package io.mpruy.gor_gemilangcondet.backend_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockCardResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockOverviewResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.StockMutation;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.StockMutationDirection;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.StockMutationSource;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.kantin.BarangKantin;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.kantin.BarangKantinStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.kantin.BarangKantinType;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ResourceNotFoundException;
import io.mpruy.gor_gemilangcondet.backend_api.repository.BarangRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.StockMutationRepository;
import io.mpruy.gor_gemilangcondet.backend_api.service.mapper.StockMapper;

@ExtendWith(MockitoExtension.class)
class StockMutationServiceTest {

    @Mock
    private StockMutationRepository stockMutationRepository;

    @Mock
    private BarangRepository barangRepository;

    @Spy
    private StockMapper stockMapper;

    @InjectMocks
    private StockMutationService stockMutationService;

    @Test
    void recordMutation_ShouldPersistMutation() {
        BarangKantin barang = BarangKantin.builder()
                .id(UUID.randomUUID())
                .name("Nasi Goreng")
                .type(BarangKantinType.MAKANAN)
                .status(BarangKantinStatus.TERSEDIA)
                .reorderThreshold(2)
                .stock(5)
                .price(15000)
                .build();

        UUID actorId = UUID.randomUUID();

        stockMutationService.recordMutation(
                barang,
                StockMutationDirection.OUT,
                2,
                StockMutationSource.MANUAL,
                "Barang rusak",
                actorId,
                5,
                3);

        ArgumentCaptor<StockMutation> captor = ArgumentCaptor.forClass(StockMutation.class);
        verify(stockMutationRepository).save(captor.capture());

        StockMutation saved = captor.getValue();
        assertEquals(barang, saved.getBarang());
        assertEquals(StockMutationDirection.OUT, saved.getDirection());
        assertEquals(2, saved.getQuantity());
        assertEquals(5, saved.getBeforeStock());
        assertEquals(3, saved.getAfterStock());
        assertEquals(StockMutationSource.MANUAL, saved.getSource());
        assertEquals("Barang rusak", saved.getReason());
        assertEquals(actorId, saved.getActorStaffId());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void getStockOverview_ShouldSortByNameAndComputeSummary() {
        BarangKantin b = BarangKantin.builder()
                .id(UUID.randomUUID())
                .name("Bakso")
                .type(BarangKantinType.MAKANAN)
                .status(BarangKantinStatus.TERSEDIA)
                .reorderThreshold(10)
                .stock(4)
                .price(12000)
                .build();

        BarangKantin a = BarangKantin.builder()
                .id(UUID.randomUUID())
                .name("Air")
                .type(BarangKantinType.MINUMAN)
                .status(BarangKantinStatus.TERSEDIA)
                .reorderThreshold(1)
                .stock(3)
                .price(5000)
                .build();

        when(barangRepository.findAllSellable()).thenReturn(List.of(b, a));

        StockOverviewResponse response = stockMutationService.getStockOverview();

        assertEquals(2, response.getSummary().getTotalProduk());
        assertEquals("Air", response.getItems().get(0).getNama());
        assertEquals(1, response.getSummary().getStokRendah());
        assertEquals((4 * 12000) + (3 * 5000), response.getSummary().getNilaiStok());
    }

    @Test
    void getStockCard_ShouldReturnExtendedCardResponse() {
        UUID barangId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();

        BarangKantin barang = BarangKantin.builder()
                .id(barangId)
                .name("Bakso")
                .type(BarangKantinType.MAKANAN)
                .status(BarangKantinStatus.TERSEDIA)
                .reorderThreshold(3)
                .stock(3)
                .price(12000)
                .build();

        StockMutation mutation = StockMutation.builder()
                .id(UUID.randomUUID())
                .barang(barang)
                .direction(StockMutationDirection.OUT)
                .quantity(1)
                .beforeStock(4)
                .afterStock(3)
                .source(StockMutationSource.MANUAL)
                .reason("Sample")
                .actorStaffId(staffId)
                .createdAt(LocalDateTime.now())
                .build();

        when(barangRepository.findById(barangId)).thenReturn(Optional.of(barang));
        when(stockMutationRepository.findByBarangIdOrderByCreatedAtDesc(barangId)).thenReturn(List.of(mutation));

        StockCardResponse response = stockMutationService.getStockCard(barangId);

        assertEquals(barangId, response.getBarangId());
        assertEquals("KNT", response.getKode().substring(0, 3));
        assertEquals("BARANG_KANTIN", response.getKategori());
        assertEquals("RENDAH", response.getStatus());
        assertEquals(1, response.getEntries().size());
        assertEquals(staffId, response.getEntries().getFirst().getStaffId());
    }

    @Test
    void getStockCard_ShouldThrowWhenBarangMissing() {
        UUID barangId = UUID.randomUUID();
        when(barangRepository.findById(barangId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> stockMutationService.getStockCard(barangId));
    }
}
