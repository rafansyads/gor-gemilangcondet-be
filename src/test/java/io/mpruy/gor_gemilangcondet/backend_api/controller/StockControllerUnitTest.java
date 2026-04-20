package io.mpruy.gor_gemilangcondet.backend_api.controller;

import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseResponseDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.requests.StockAdjustmentRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockCardResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockItemResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockOverviewResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.stocks.responses.StockSummaryResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.stocks.StockMutationDirection;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.Role;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.RoleName;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.User;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatusName;
import io.mpruy.gor_gemilangcondet.backend_api.exception.BadRequestException;
import io.mpruy.gor_gemilangcondet.backend_api.security.UserDetailsImpl;
import io.mpruy.gor_gemilangcondet.backend_api.service.StockService;
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

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockControllerUnitTest {

    @Mock
    private StockService stockService;

    @InjectMocks
    private StockController stockController;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private UUID authenticateAsStaff() {
        UUID userId = UUID.randomUUID();
        Role role = Role.builder().roleName(RoleName.STAF_TOKO).build();
        UserStatus status = UserStatus.builder().name(UserStatusName.AKTIF).build();
        User user = User.builder()
                .id(userId)
                .username("staff")
                .email("staff@example.com")
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
    @DisplayName("getOverview should return 200 with stock data")
    void getOverview_Success() {
        StockOverviewResponse overview = StockOverviewResponse.builder()
                .summary(StockSummaryResponse.builder().totalProduk(1).stokRendah(0).nilaiStok(10000).build())
                .items(Collections.emptyList())
                .build();
        when(stockService.getStockOverview()).thenReturn(overview);

        ResponseEntity<BaseResponseDto<StockOverviewResponse>> response = stockController.getOverview();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(overview, response.getBody().getData());
    }

    @Test
    @DisplayName("adjustStock should return 200 when authenticated")
    void adjustStock_Success() {
        UUID userId = authenticateAsStaff();

        StockAdjustmentRequest request = new StockAdjustmentRequest();
        request.setBarangId(UUID.randomUUID());
        request.setDirection(StockMutationDirection.IN);
        request.setQuantity(3);
        request.setReason("Restock");

        StockItemResponse item = StockItemResponse.builder()
                .barangId(request.getBarangId())
                .nama("Raket")
                .stok(12)
                .build();

        when(stockService.adjustStock(request, userId)).thenReturn(item);

        ResponseEntity<BaseResponseDto<StockItemResponse>> response = stockController.adjustStock(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(item, response.getBody().getData());
        verify(stockService).adjustStock(request, userId);
    }

    @Test
    @DisplayName("adjustStock should throw when unauthenticated")
    void adjustStock_Unauthenticated() {
        SecurityContextHolder.clearContext();

        StockAdjustmentRequest request = new StockAdjustmentRequest();
        request.setBarangId(UUID.randomUUID());
        request.setDirection(StockMutationDirection.IN);
        request.setQuantity(1);
        request.setReason("Manual");

        assertThrows(BadRequestException.class, () -> stockController.adjustStock(request));
    }

    @Test
    @DisplayName("getStockCard should return 200 with card details")
    void getStockCard_Success() {
        UUID barangId = UUID.randomUUID();
        StockCardResponse card = StockCardResponse.builder()
                .barangId(barangId)
                .namaBarang("Raket")
                .stok(8)
                .entries(Collections.emptyList())
                .build();

        when(stockService.getStockCard(barangId)).thenReturn(card);

        ResponseEntity<BaseResponseDto<StockCardResponse>> response = stockController.getStockCard(barangId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(card, response.getBody().getData());
    }

    @Test
    @DisplayName("exportOverviewCsv should return downloadable csv")
    void exportOverviewCsv_Success() {
        byte[] csv = "name,stock\nRaket,8\n".getBytes();
        when(stockService.exportStockOverviewCsv()).thenReturn(csv);

        ResponseEntity<byte[]> response = stockController.exportOverviewCsv();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("text/csv", response.getHeaders().getContentType().toString());
        assertEquals("stock-overview.csv", response.getHeaders().getContentDisposition().getFilename());
        assertArrayEquals(csv, response.getBody());
    }

    @Test
    @DisplayName("exportStockCardCsv should return downloadable csv")
    void exportStockCardCsv_Success() {
        UUID barangId = UUID.randomUUID();
        byte[] csv = "time,direction,qty\n".getBytes();
        when(stockService.exportStockCardCsv(barangId)).thenReturn(csv);

        ResponseEntity<byte[]> response = stockController.exportStockCardCsv(barangId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("text/csv", response.getHeaders().getContentType().toString());
        assertEquals("stock-card-" + barangId + ".csv", response.getHeaders().getContentDisposition().getFilename());
        assertArrayEquals(csv, response.getBody());
    }
}
