package io.mpruy.gor_gemilangcondet.backend_api.service;

import io.mpruy.gor_gemilangcondet.backend_api.dto.transaksi.requests.CartItemRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.transaksi.requests.CheckoutRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.transaksi.responses.TransaksiResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.payment.PaymentMethod;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.StockMutationDirection;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.StockMutationSource;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.toko.BarangToko;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.toko.BarangTokoStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.toko.BarangTokoType;
import io.mpruy.gor_gemilangcondet.backend_api.entities.transaksi.Transaksi;
import io.mpruy.gor_gemilangcondet.backend_api.entities.transaksi.TransaksiStatus;
import io.mpruy.gor_gemilangcondet.backend_api.exception.BadRequestException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ResourceNotFoundException;
import io.mpruy.gor_gemilangcondet.backend_api.repository.BarangRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.TransaksiRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransaksiServiceTest {

    @Mock
    private TransaksiRepository transaksiRepository;
    @Mock
    private BarangRepository barangRepository;
    @Mock
    private StockService stockService;
    @Mock
    private StockMutationService stockMutationService;

    @InjectMocks
    private TransaksiService transaksiService;

    private CheckoutRequest buildCheckoutRequest(UUID barangId, int quantity, double diskon) {
        CartItemRequest item = new CartItemRequest();
        item.setBarangId(barangId);
        item.setKuantitas(quantity);

        CheckoutRequest request = new CheckoutRequest();
        request.setPaymentMethod(PaymentMethod.CASH);
        request.setItems(List.of(item));
        request.setDiskon(diskon);
        return request;
    }

    private BarangToko buildBarang(UUID id, long stock, double price) {
        return BarangToko.builder()
                .id(id)
                .name("Raket Yonex")
                .stock(stock)
                .price(price)
                .unit("pcs")
                .type(BarangTokoType.ALAT_OLAHRAGA)
                .status(BarangTokoStatus.TERSEDIA)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("checkout should create transaction and reduce stock")
    void checkout_Success() {
        UUID barangId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();

        CheckoutRequest request = buildCheckoutRequest(barangId, 2, 5000);
        BarangToko barang = buildBarang(barangId, 10, 100000);

        when(barangRepository.findByIdWithPessimisticLock(barangId)).thenReturn(Optional.of(barang));
        when(transaksiRepository.save(any(Transaksi.class))).thenAnswer(invocation -> {
            Transaksi saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        TransaksiResponse response = transaksiService.checkout(request, staffId);

        assertNotNull(response.getId());
        assertEquals(200000, response.getSubtotal());
        assertEquals(5000, response.getDiskon());
        assertEquals(195000, response.getGrandTotal());
        assertEquals(TransaksiStatus.SELESAI, response.getStatus());
        assertEquals(1, response.getItems().size());

        verify(stockMutationService, times(1)).recordMutation(
                eq(barang),
                eq(StockMutationDirection.OUT),
                eq(2L),
                eq(StockMutationSource.POS),
                eq("Checkout transaksi"),
                eq(staffId),
                eq(10L),
                eq(8L));
        verify(transaksiRepository, times(1)).save(any(Transaksi.class));
    }

    @Test
    @DisplayName("checkout should throw when product is not found")
    void checkout_BarangNotFound() {
        UUID barangId = UUID.randomUUID();
        CheckoutRequest request = buildCheckoutRequest(barangId, 1, 0);

        when(barangRepository.findByIdWithPessimisticLock(barangId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> transaksiService.checkout(request, UUID.randomUUID()));
        verify(transaksiRepository, never()).save(any(Transaksi.class));
    }

    @Test
    @DisplayName("checkout should throw when stock is insufficient")
    void checkout_InsufficientStock() {
        UUID barangId = UUID.randomUUID();
        CheckoutRequest request = buildCheckoutRequest(barangId, 5, 0);
        BarangToko barang = buildBarang(barangId, 2, 75000);

        when(barangRepository.findByIdWithPessimisticLock(barangId)).thenReturn(Optional.of(barang));

        assertThrows(BadRequestException.class, () -> transaksiService.checkout(request, UUID.randomUUID()));
        verify(transaksiRepository, never()).save(any(Transaksi.class));
    }

    @Test
    @DisplayName("checkout should throw when discount exceeds subtotal")
    void checkout_DiscountExceedsSubtotal() {
        UUID barangId = UUID.randomUUID();
        CheckoutRequest request = buildCheckoutRequest(barangId, 1, 200000);
        BarangToko barang = buildBarang(barangId, 4, 100000);

        when(barangRepository.findByIdWithPessimisticLock(barangId)).thenReturn(Optional.of(barang));

        assertThrows(BadRequestException.class, () -> transaksiService.checkout(request, UUID.randomUUID()));
        verify(transaksiRepository, never()).save(any(Transaksi.class));
    }

    @Test
    @DisplayName("getAllTransaksi should return mapped responses")
    void getAllTransaksi_Success() {
        UUID id = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();

        BarangToko barang = buildBarang(UUID.randomUUID(), 3, 50000);
        Transaksi transaksi = Transaksi.builder()
                .id(id)
                .staffId(staffId)
                .paymentMethod(PaymentMethod.QRIS)
                .status(TransaksiStatus.SELESAI)
                .subtotal(100000)
                .diskon(10000)
                .grandTotal(90000)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        transaksi.getDetails().add(
                io.mpruy.gor_gemilangcondet.backend_api.entities.transaksi.TransaksiDetail.builder()
                        .id(UUID.randomUUID())
                        .transaksi(transaksi)
                        .barang(barang)
                        .hargaSatuan(50000)
                        .kuantitas(2)
                        .subtotal(100000)
                        .build());

        when(transaksiRepository.findAllWithDetails()).thenReturn(List.of(transaksi));

        List<TransaksiResponse> responses = transaksiService.getAllTransaksi();

        assertEquals(1, responses.size());
        assertEquals(id, responses.get(0).getId());
        assertEquals(1, responses.get(0).getItems().size());
        assertEquals("Raket Yonex", responses.get(0).getItems().get(0).getBarangName());
    }

    @Test
    @DisplayName("getTransaksiById should throw when transaction does not exist")
    void getTransaksiById_NotFound() {
        UUID id = UUID.randomUUID();
        when(transaksiRepository.findByIdWithDetails(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> transaksiService.getTransaksiById(id));
    }

    @Test
    @DisplayName("getTransaksiById should return mapped response")
    void getTransaksiById_Success() {
        UUID id = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();

        Transaksi transaksi = Transaksi.builder()
                .id(id)
                .staffId(staffId)
                .paymentMethod(PaymentMethod.TRANSFER)
                .status(TransaksiStatus.SELESAI)
                .subtotal(250000)
                .diskon(0)
                .grandTotal(250000)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(transaksiRepository.findByIdWithDetails(id)).thenReturn(Optional.of(transaksi));

        TransaksiResponse response = transaksiService.getTransaksiById(id);

        assertEquals(id, response.getId());
        assertEquals(PaymentMethod.TRANSFER, response.getPaymentMethod());
        assertEquals(250000, response.getGrandTotal());
    }
}
