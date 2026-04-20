package io.mpruy.gor_gemilangcondet.backend_api.controller;

import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseResponseDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.ConfirmPaymentResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.InvoiceResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.PembayaranResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.ReservasiResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.payment.PaymentStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.LapanganType;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.ReservasiStatus;
import io.mpruy.gor_gemilangcondet.backend_api.service.PembayaranService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PembayaranControllerUnitTest {

    @Mock
    private PembayaranService pembayaranService;

    @InjectMocks
    private PembayaranController pembayaranController;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pembayaranController, "uploadDir", "uploads/payment-proofs-test");
    }

    private ReservasiResponse buildReservasiResponse(UUID id) {
        return ReservasiResponse.builder()
                .id(id)
                .userId(UUID.randomUUID())
                .lapanganId(UUID.randomUUID())
                .lapanganName("Badminton 1")
                .lapanganType(LapanganType.BADMINTON)
                .reservationStart(LocalDateTime.now().plusDays(1))
                .reservationEnd(LocalDateTime.now().plusDays(1).plusHours(2))
                .durationInHours(2)
                .status(ReservasiStatus.BELUM_DIBAYAR)
                .totalPayment(100000)
                .build();
    }

    @Test
    @DisplayName("getUnpaidReservations should return 200")
    void getUnpaidReservations_Success() {
        when(pembayaranService.getUnpaidReservations()).thenReturn(List.of(buildReservasiResponse(UUID.randomUUID())));

        ResponseEntity<BaseResponseDto<List<ReservasiResponse>>> response = pembayaranController.getUnpaidReservations();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    @DisplayName("getStaffReservations should return 200")
    void getStaffReservations_Success() {
        when(pembayaranService.getReservationsForStaff()).thenReturn(List.of(buildReservasiResponse(UUID.randomUUID())));

        ResponseEntity<BaseResponseDto<List<ReservasiResponse>>> response = pembayaranController.getStaffReservations();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    @DisplayName("getStaffInvoices should return 200")
    void getStaffInvoices_Success() {
        InvoiceResponse invoice = InvoiceResponse.builder()
                .invoiceId(UUID.randomUUID().toString())
                .invoiceNumber("INV-20260420-001")
                .representativeName("John")
                .totalAmount(150000)
                .slotCount(2)
                .status("BELUM_DIBAYAR")
                .createdAt(LocalDateTime.now())
                .build();

        when(pembayaranService.getInvoicesForStaff()).thenReturn(List.of(invoice));

        ResponseEntity<BaseResponseDto<List<InvoiceResponse>>> response = pembayaranController.getStaffInvoices();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    @DisplayName("getPaymentByReservation should return 200")
    void getPaymentByReservation_Success() {
        UUID reservationId = UUID.randomUUID();
        PembayaranResponse pembayaran = PembayaranResponse.builder()
                .id(UUID.randomUUID())
                .reservationId(reservationId)
                .status(PaymentStatus.BELUM_DIBAYAR)
                .price(100000)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(pembayaranService.getPaymentByReservationId(reservationId)).thenReturn(pembayaran);

        ResponseEntity<BaseResponseDto<PembayaranResponse>> response =
                pembayaranController.getPaymentByReservation(reservationId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(reservationId, response.getBody().getData().getReservationId());
    }

    @Test
    @DisplayName("uploadPaymentProof should return 200")
    void uploadPaymentProof_Success() {
        UUID reservationId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "proof.jpg", "image/jpeg", "img".getBytes());

        ReservasiResponse updated = buildReservasiResponse(reservationId);
        updated.setStatus(ReservasiStatus.MENUNGGU_KONFIRMASI_STAF);
        when(pembayaranService.uploadPaymentProof(reservationId, file)).thenReturn(updated);

        ResponseEntity<BaseResponseDto<ReservasiResponse>> response =
                pembayaranController.uploadPaymentProof(reservationId, file);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(ReservasiStatus.MENUNGGU_KONFIRMASI_STAF, response.getBody().getData().getStatus());
    }

    @Test
    @DisplayName("confirmPayment should return 200")
    void confirmPayment_Success() {
        UUID reservationId = UUID.randomUUID();
        ConfirmPaymentResponse payload = ConfirmPaymentResponse.builder()
                .reservationId(reservationId)
                .reservationStatus(ReservasiStatus.DIKONFIRMASI)
                .paymentStatus(PaymentStatus.LUNAS)
                .lapanganName("Badminton 1")
                .message("ok")
                .build();

        when(pembayaranService.confirmPayment(reservationId)).thenReturn(payload);

        ResponseEntity<BaseResponseDto<ConfirmPaymentResponse>> response = pembayaranController.confirmPayment(reservationId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(ReservasiStatus.DIKONFIRMASI, response.getBody().getData().getReservationStatus());
    }

    @Test
    @DisplayName("rejectPayment should return 200")
    void rejectPayment_Success() {
        UUID reservationId = UUID.randomUUID();
        ConfirmPaymentResponse payload = ConfirmPaymentResponse.builder()
                .reservationId(reservationId)
                .reservationStatus(ReservasiStatus.DITOLAK)
            .paymentStatus(PaymentStatus.DIBATALKAN)
                .lapanganName("Badminton 1")
                .message("rejected")
                .build();

        when(pembayaranService.rejectPayment(reservationId)).thenReturn(payload);

        ResponseEntity<BaseResponseDto<ConfirmPaymentResponse>> response = pembayaranController.rejectPayment(reservationId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(ReservasiStatus.DITOLAK, response.getBody().getData().getReservationStatus());
    }

    @Test
    @DisplayName("getPaymentProof should return 404 when file is missing")
    void getPaymentProof_NotFound() throws Exception {
        ResponseEntity<Resource> response = pembayaranController.getPaymentProof("missing-proof.jpg");

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    @DisplayName("getPaymentProof should map extension to expected image content types")
    void getPaymentProof_ContentTypeMapping() throws Exception {
        String[][] cases = {
                { "proof.png", "image/png" },
                { "proof.webp", "image/webp" },
                { "proof.heic", "image/heic" },
                { "proof.heif", "image/heif" },
                { "proof.bmp", "image/bmp" },
                { "proof.jpg", "image/jpeg" }
        };

        Path dir = Paths.get("uploads/payment-proofs-test");
        Files.createDirectories(dir);

        for (String[] c : cases) {
            Path filePath = dir.resolve(c[0]);
            Files.write(filePath, "img".getBytes());
            try {
                ResponseEntity<Resource> response = pembayaranController.getPaymentProof(c[0]);
                assertEquals(200, response.getStatusCode().value());
                assertEquals(c[1], response.getHeaders().getContentType().toString());
                assertTrue(response.getHeaders().getFirst("Content-Disposition").contains(c[0]));
            } finally {
                Files.deleteIfExists(filePath);
            }
        }
    }
}
