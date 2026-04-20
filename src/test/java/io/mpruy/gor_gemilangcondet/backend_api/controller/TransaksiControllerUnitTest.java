package io.mpruy.gor_gemilangcondet.backend_api.controller;

import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseResponseDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.transaksi.requests.CartItemRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.transaksi.requests.CheckoutRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.transaksi.responses.TransaksiDetailResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.transaksi.responses.TransaksiResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.payment.PaymentMethod;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.Barang;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.toko.BarangToko;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.toko.BarangTokoStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.toko.BarangTokoType;
import io.mpruy.gor_gemilangcondet.backend_api.entities.transaksi.TransaksiStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.Role;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.RoleName;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.User;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatusName;
import io.mpruy.gor_gemilangcondet.backend_api.exception.BadRequestException;
import io.mpruy.gor_gemilangcondet.backend_api.repository.BarangRepository;
import io.mpruy.gor_gemilangcondet.backend_api.security.UserDetailsImpl;
import io.mpruy.gor_gemilangcondet.backend_api.service.TransaksiService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransaksiControllerUnitTest {

    @Mock
    private TransaksiService transaksiService;
    @Mock
    private BarangRepository barangRepository;

    @InjectMocks
    private TransaksiController transaksiController;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private UUID authenticateAsStaff() {
        UUID userId = UUID.randomUUID();
        Role role = Role.builder().roleName(RoleName.STAF_LAPANGAN).build();
        UserStatus status = UserStatus.builder().name(UserStatusName.AKTIF).build();
        User user = User.builder()
                .id(userId)
                .username("cashier")
                .email("cashier@example.com")
                .password("secret")
                .role(role)
                .status(status)
                .build();
        UserDetailsImpl principal = new UserDetailsImpl(user);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        return userId;
    }

    @Test
    @DisplayName("getAllBarang should return 200 with sellable items")
    void getAllBarang_Success() {
        Barang barang = BarangToko.builder()
                .id(UUID.randomUUID())
                .name("Raket")
                .price(500000)
                .stock(5)
                .unit("pcs")
                .type(BarangTokoType.ALAT_OLAHRAGA)
                .status(BarangTokoStatus.TERSEDIA)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(barangRepository.findAllSellable()).thenReturn(List.of(barang));

        ResponseEntity<BaseResponseDto<List<Barang>>> response = transaksiController.getAllBarang();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
        assertEquals("Raket", response.getBody().getData().get(0).getName());
    }

    @Test
    @DisplayName("checkout should return 201 for authenticated staff")
    void checkout_Success() {
        UUID staffId = authenticateAsStaff();
        UUID barangId = UUID.randomUUID();

        CartItemRequest item = new CartItemRequest();
        item.setBarangId(barangId);
        item.setKuantitas(2);

        CheckoutRequest request = new CheckoutRequest();
        request.setPaymentMethod(PaymentMethod.CASH);
        request.setItems(List.of(item));
        request.setDiskon(0);

        TransaksiResponse transaksiResponse = TransaksiResponse.builder()
                .id(UUID.randomUUID())
                .staffId(staffId)
                .paymentMethod(PaymentMethod.CASH)
                .status(TransaksiStatus.SELESAI)
                .subtotal(100000)
                .diskon(0)
                .grandTotal(100000)
                .items(List.of(TransaksiDetailResponse.builder()
                        .id(UUID.randomUUID())
                        .barangId(barangId)
                        .barangName("Raket")
                        .hargaSatuan(50000)
                        .kuantitas(2)
                        .subtotal(100000)
                        .build()))
                .createdAt(LocalDateTime.now())
                .build();

        when(transaksiService.checkout(request, staffId)).thenReturn(transaksiResponse);

        ResponseEntity<BaseResponseDto<TransaksiResponse>> response = transaksiController.checkout(request);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(transaksiResponse, response.getBody().getData());
        verify(transaksiService).checkout(request, staffId);
    }

    @Test
    @DisplayName("checkout should throw for unauthenticated principal")
    void checkout_Unauthenticated() {
        SecurityContextHolder.clearContext();

        CheckoutRequest request = new CheckoutRequest();
        request.setPaymentMethod(PaymentMethod.QRIS);
        request.setItems(Collections.emptyList());
        request.setDiskon(0);

        assertThrows(BadRequestException.class, () -> transaksiController.checkout(request));
    }

    @Test
    @DisplayName("getAllTransaksi should return 200")
    void getAllTransaksi_Success() {
        when(transaksiService.getAllTransaksi()).thenReturn(Collections.emptyList());

        ResponseEntity<BaseResponseDto<List<TransaksiResponse>>> response = transaksiController.getAllTransaksi();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(0, response.getBody().getData().size());
    }

    @Test
    @DisplayName("getTransaksiById should return 200 with transaction details")
    void getTransaksiById_Success() {
        UUID transaksiId = UUID.randomUUID();
        TransaksiResponse transaksiResponse = TransaksiResponse.builder()
                .id(transaksiId)
                .paymentMethod(PaymentMethod.TRANSFER)
                .status(TransaksiStatus.SELESAI)
                .subtotal(250000)
                .diskon(10000)
                .grandTotal(240000)
                .items(Collections.emptyList())
                .createdAt(LocalDateTime.now())
                .build();

        when(transaksiService.getTransaksiById(transaksiId)).thenReturn(transaksiResponse);

        ResponseEntity<BaseResponseDto<TransaksiResponse>> response = transaksiController.getTransaksiById(transaksiId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(transaksiId, response.getBody().getData().getId());
    }
}
