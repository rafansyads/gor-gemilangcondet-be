package io.mpruy.gor_gemilangcondet.backend_api.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockCardEntryResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockCardResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockItemResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockOverviewResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockSummaryResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.Barang;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.StockMutation;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.StockMutationDirection;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.StockMutationSource;
import io.mpruy.gor_gemilangcondet.backend_api.repository.BarangRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.StockMutationRepository;
import io.mpruy.gor_gemilangcondet.backend_api.service.mapper.StockMapper;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockMutationService {

    private final StockMutationRepository stockMutationRepository;
    private final BarangRepository barangRepository;
    private final StockMapper stockMapper;

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

    @Transactional(readOnly = true)
    public StockOverviewResponse getStockOverview() {
        List<StockItemResponse> items = barangRepository.findAllSellable().stream()
                .sorted(Comparator.comparing(Barang::getName))
                .map(stockMapper::toItemResponse)
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

    @Transactional(readOnly = true)
    public StockCardResponse getStockCard(UUID barangId) {
        Barang barang = barangRepository.findById(barangId)
                .orElseThrow(() -> new ResourceNotFoundException("Barang tidak ditemukan: " + barangId));

        List<StockCardEntryResponse> entries = stockMutationRepository.findByBarangIdOrderByCreatedAtDesc(barangId)
                .stream()
                .map(stockMapper::toCardEntryResponse)
                .toList();

        return stockMapper.toCardResponse(barang, entries);
    }
}