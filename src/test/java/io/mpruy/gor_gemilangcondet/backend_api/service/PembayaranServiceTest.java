package io.mpruy.gor_gemilangcondet.backend_api.service;

import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.ConfirmPaymentResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.PembayaranResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.ReservasiResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.payment.PaymentStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.payment.Pembayaran;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.*;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.Role;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.RoleName;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.User;
import io.mpruy.gor_gemilangcondet.backend_api.exception.BadRequestException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ConflictException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ResourceNotFoundException;
import io.mpruy.gor_gemilangcondet.backend_api.repository.LapanganRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.PembayaranRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.ReservasiRepository;
import io.mpruy.gor_gemilangcondet.backend_api.security.UserDetailsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PembayaranServiceTest {

    @Mock
    private PembayaranRepository pembayaranRepository;
    @Mock
    private ReservasiRepository reservasiRepository;
    @Mock
    private LapanganRepository lapanganRepository;

    @InjectMocks
    private PembayaranService pembayaranService;

    private UUID reservasiId;
    private UUID userId;
    private Lapangan testCourt;
    private Reservasi testReservasi;

    @BeforeEach
    void setUp() {
        reservasiId = UUID.randomUUID();
        userId = UUID.randomUUID();
        testCourt = Lapangan.builder()
                .id(UUID.randomUUID())
                .name("Badminton 1")
                .type(LapanganType.BADMINTON)
                .status(LapanganStatus.TERSEDIA)
                .tarifPerJam(50000)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testReservasi = Reservasi.builder()
                .id(reservasiId)
                .userId(userId)
                .lapangan(testCourt)
                .reservationStart(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0))
                .reservationEnd(LocalDateTime.now().plusDays(1).withHour(12).withMinute(0))
                .namaWakil("John")
                .nomorTelepon("08123")
                .jumlahOrang(4)
                .totalPayment(100000)
                .status(ReservasiStatus.BELUM_DIBAYAR)
                .paymentDeadline(LocalDateTime.now().plusDays(1))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setUpStaffAuth() {
        UUID staffId = UUID.randomUUID();
        Role staffRole = new Role();
        staffRole.setId(1);
        staffRole.setRoleName(RoleName.STAF_LAPANGAN);
        User staffUser = User.builder()
                .id(staffId).username("staff1").email("staff@test.com")
                .password("Pass1234").role(staffRole).build();
        UserDetailsImpl userDetails = new UserDetailsImpl(staffUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null,
                userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET UNPAID RESERVATIONS
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Get Unpaid Reservations Tests")
    class GetUnpaidReservationsTests {

        @Test
        @DisplayName("Should return unpaid reservations")
        void getUnpaidReservations_Success() {
            when(reservasiRepository.findByStatusWithLapangan(ReservasiStatus.BELUM_DIBAYAR))
                    .thenReturn(List.of(testReservasi));

            List<ReservasiResponse> result = pembayaranService.getUnpaidReservations();

            assertEquals(1, result.size());
            assertEquals(reservasiId, result.get(0).getId());
        }

        @Test
        @DisplayName("Should return empty when no unpaid reservations")
        void getUnpaidReservations_Empty() {
            when(reservasiRepository.findByStatusWithLapangan(ReservasiStatus.BELUM_DIBAYAR))
                    .thenReturn(Collections.emptyList());

            List<ReservasiResponse> result = pembayaranService.getUnpaidReservations();

            assertTrue(result.isEmpty());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET RESERVATIONS FOR STAFF
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Get Reservations For Staff Tests")
    class GetReservationsForStaffTests {

        @Test
        @DisplayName("Should return staff dashboard reservations")
        void getReservationsForStaff_Success() {
            testReservasi.setStatus(ReservasiStatus.MENUNGGU_KONFIRMASI_STAF);
            when(reservasiRepository.findByStatusInWithLapangan(anyList()))
                    .thenReturn(List.of(testReservasi));

            List<ReservasiResponse> result = pembayaranService.getReservationsForStaff();

            assertEquals(1, result.size());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET PAYMENT BY RESERVATION ID
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Get Payment By Reservation ID Tests")
    class GetPaymentTests {

        @Test
        @DisplayName("Should return payment by reservation ID")
        void getPaymentByReservationId_Success() {
            Pembayaran pembayaran = Pembayaran.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .staffId(UUID.randomUUID())
                    .reservationId(reservasiId)
                    .status(PaymentStatus.LUNAS)
                    .price(100000)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(pembayaranRepository.findByReservationId(reservasiId))
                    .thenReturn(Optional.of(pembayaran));

            PembayaranResponse result = pembayaranService.getPaymentByReservationId(reservasiId);

            assertNotNull(result);
            assertEquals(reservasiId, result.getReservationId());
        }

        @Test
        @DisplayName("Should throw when payment not found")
        void getPaymentByReservationId_NotFound() {
            UUID randomId = UUID.randomUUID();
            when(pembayaranRepository.findByReservationId(randomId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> pembayaranService.getPaymentByReservationId(randomId));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UPLOAD PAYMENT PROOF
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Upload Payment Proof Tests")
    class UploadPaymentProofTests {

        @Test
        @DisplayName("Should reject when reservation not found")
        void uploadProof_ReservationNotFound() {
            UUID randomId = UUID.randomUUID();
            MultipartFile file = mock(MultipartFile.class);
            when(reservasiRepository.findByIdWithLapangan(randomId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> pembayaranService.uploadPaymentProof(randomId, file));
        }

        @Test
        @DisplayName("Should reject when status is not BELUM_DIBAYAR")
        void uploadProof_WrongStatus() {
            testReservasi.setStatus(ReservasiStatus.DIKONFIRMASI);
            MultipartFile file = mock(MultipartFile.class);
            when(reservasiRepository.findByIdWithLapangan(reservasiId))
                    .thenReturn(Optional.of(testReservasi));

            assertThrows(ConflictException.class,
                    () -> pembayaranService.uploadPaymentProof(reservasiId, file));
        }

        @Test
        @DisplayName("Should reject when deadline has passed")
        void uploadProof_DeadlinePassed() {
            testReservasi.setPaymentDeadline(LocalDateTime.now().minusHours(1));
            MultipartFile file = mock(MultipartFile.class);
            when(reservasiRepository.findByIdWithLapangan(reservasiId))
                    .thenReturn(Optional.of(testReservasi));

            assertThrows(ConflictException.class,
                    () -> pembayaranService.uploadPaymentProof(reservasiId, file));
        }

        @Test
        @DisplayName("Should reject when file is null")
        void uploadProof_NullFile() {
            when(reservasiRepository.findByIdWithLapangan(reservasiId))
                    .thenReturn(Optional.of(testReservasi));

            assertThrows(BadRequestException.class,
                    () -> pembayaranService.uploadPaymentProof(reservasiId, null));
        }

        @Test
        @DisplayName("Should reject when file is empty")
        void uploadProof_EmptyFile() {
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(true);
            when(reservasiRepository.findByIdWithLapangan(reservasiId))
                    .thenReturn(Optional.of(testReservasi));

            assertThrows(BadRequestException.class,
                    () -> pembayaranService.uploadPaymentProof(reservasiId, file));
        }

        @Test
        @DisplayName("Should upload proof successfully")
        void uploadProof_Success() throws IOException {
            ReflectionTestUtils.setField(pembayaranService, "uploadDir", "build/test-uploads/payment-proofs");

            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getOriginalFilename()).thenReturn("receipt.jpg");
            when(file.getInputStream()).thenReturn(new ByteArrayInputStream("fake image".getBytes()));

            when(reservasiRepository.findByIdWithLapangan(reservasiId))
                    .thenReturn(Optional.of(testReservasi));
            when(reservasiRepository.save(any(Reservasi.class))).thenAnswer(i -> i.getArgument(0));

            ReservasiResponse result = pembayaranService.uploadPaymentProof(reservasiId, file);

            assertNotNull(result);
            assertEquals(ReservasiStatus.MENUNGGU_KONFIRMASI_STAF, result.getStatus());
        }

        @Test
        @DisplayName("Should reject file with unsupported extension")
        void uploadProof_UnsupportedExtension() {
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getOriginalFilename()).thenReturn("document.pdf");

            when(reservasiRepository.findByIdWithLapangan(reservasiId))
                    .thenReturn(Optional.of(testReservasi));

            assertThrows(BadRequestException.class,
                    () -> pembayaranService.uploadPaymentProof(reservasiId, file));
        }

        @Test
        @DisplayName("Should reject file with mismatched content type")
        void uploadProof_UnsupportedContentType() {
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getOriginalFilename()).thenReturn("photo.jpg");
            when(file.getContentType()).thenReturn("application/pdf");

            when(reservasiRepository.findByIdWithLapangan(reservasiId))
                    .thenReturn(Optional.of(testReservasi));

            assertThrows(BadRequestException.class,
                    () -> pembayaranService.uploadPaymentProof(reservasiId, file));
        }

        @Test
        @DisplayName("Should accept HEIC photo format")
        void uploadProof_HeicFormatAccepted() throws IOException {
            ReflectionTestUtils.setField(pembayaranService, "uploadDir", "build/test-uploads/payment-proofs");

            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getOriginalFilename()).thenReturn("photo.heic");
            when(file.getContentType()).thenReturn("image/heic");
            when(file.getInputStream()).thenReturn(new ByteArrayInputStream("fake image".getBytes()));

            when(reservasiRepository.findByIdWithLapangan(reservasiId))
                    .thenReturn(Optional.of(testReservasi));
            when(reservasiRepository.save(any(Reservasi.class))).thenAnswer(i -> i.getArgument(0));

            ReservasiResponse result = pembayaranService.uploadPaymentProof(reservasiId, file);

            assertNotNull(result);
            assertEquals(ReservasiStatus.MENUNGGU_KONFIRMASI_STAF, result.getStatus());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CONFIRM PAYMENT
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Confirm Payment Tests")
    class ConfirmPaymentTests {

        @Test
        @DisplayName("Should confirm payment successfully")
        void confirmPayment_Success() {
            setUpStaffAuth();
            testReservasi.setStatus(ReservasiStatus.MENUNGGU_KONFIRMASI_STAF);
            testReservasi.setPaymentProofUrl("proof.jpg");

            when(reservasiRepository.findByIdWithLapangan(reservasiId))
                    .thenReturn(Optional.of(testReservasi));
            when(pembayaranRepository.save(any(Pembayaran.class))).thenAnswer(i -> {
                Pembayaran p = i.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });
            when(reservasiRepository.save(any(Reservasi.class))).thenAnswer(i -> i.getArgument(0));
            when(lapanganRepository.save(any(Lapangan.class))).thenAnswer(i -> i.getArgument(0));

            ConfirmPaymentResponse result = pembayaranService.confirmPayment(reservasiId);

            assertNotNull(result);
            assertEquals(ReservasiStatus.DIKONFIRMASI, result.getReservationStatus());
            assertEquals(PaymentStatus.LUNAS, result.getPaymentStatus());
        }

        @Test
        @DisplayName("Should throw when reservation not found")
        void confirmPayment_NotFound() {
            setUpStaffAuth();
            UUID randomId = UUID.randomUUID();
            when(reservasiRepository.findByIdWithLapangan(randomId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> pembayaranService.confirmPayment(randomId));
        }

        @Test
        @DisplayName("Should reject already confirmed reservation")
        void confirmPayment_AlreadyConfirmed() {
            testReservasi.setStatus(ReservasiStatus.DIKONFIRMASI);
            when(reservasiRepository.findByIdWithLapangan(reservasiId))
                    .thenReturn(Optional.of(testReservasi));

            assertThrows(ConflictException.class,
                    () -> pembayaranService.confirmPayment(reservasiId));
        }

        @Test
        @DisplayName("Should reject already rejected reservation")
        void confirmPayment_AlreadyRejected() {
            testReservasi.setStatus(ReservasiStatus.DITOLAK);
            when(reservasiRepository.findByIdWithLapangan(reservasiId))
                    .thenReturn(Optional.of(testReservasi));

            assertThrows(ConflictException.class,
                    () -> pembayaranService.confirmPayment(reservasiId));
        }

        @Test
        @DisplayName("Should reject cancelled reservation")
        void confirmPayment_Cancelled() {
            testReservasi.setStatus(ReservasiStatus.DIBATALKAN);
            when(reservasiRepository.findByIdWithLapangan(reservasiId))
                    .thenReturn(Optional.of(testReservasi));

            assertThrows(ConflictException.class,
                    () -> pembayaranService.confirmPayment(reservasiId));
        }

        @Test
        @DisplayName("Should reject expired reservation")
        void confirmPayment_Expired() {
            testReservasi.setStatus(ReservasiStatus.EXPIRED);
            when(reservasiRepository.findByIdWithLapangan(reservasiId))
                    .thenReturn(Optional.of(testReservasi));

            assertThrows(ConflictException.class,
                    () -> pembayaranService.confirmPayment(reservasiId));
        }

        @Test
        @DisplayName("Should reject for wrong status (not MENUNGGU_KONFIRMASI_STAF)")
        void confirmPayment_WrongStatus() {
            testReservasi.setStatus(ReservasiStatus.BELUM_DIBAYAR);
            when(reservasiRepository.findByIdWithLapangan(reservasiId))
                    .thenReturn(Optional.of(testReservasi));

            assertThrows(ConflictException.class,
                    () -> pembayaranService.confirmPayment(reservasiId));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // REJECT PAYMENT
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Reject Payment Tests")
    class RejectPaymentTests {

        @Test
        @DisplayName("Should reject payment successfully")
        void rejectPayment_Success() {
            setUpStaffAuth();
            testReservasi.setStatus(ReservasiStatus.MENUNGGU_KONFIRMASI_STAF);

            lenient().when(reservasiRepository.findByIdWithLapangan(reservasiId))
                    .thenReturn(Optional.of(testReservasi));
            lenient().when(reservasiRepository.save(any(Reservasi.class))).thenAnswer(i -> i.getArgument(0));
            lenient().when(lapanganRepository.save(any(Lapangan.class))).thenAnswer(i -> i.getArgument(0));

            ConfirmPaymentResponse result = pembayaranService.rejectPayment(reservasiId);

            assertNotNull(result);
            assertEquals(ReservasiStatus.DITOLAK, result.getReservationStatus());
        }

        @Test
        @DisplayName("Should throw when reservation not found")
        void rejectPayment_NotFound() {
            setUpStaffAuth();
            UUID randomId = UUID.randomUUID();
            when(reservasiRepository.findByIdWithLapangan(randomId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> pembayaranService.rejectPayment(randomId));
        }

        @Test
        @DisplayName("Should reject when status is not MENUNGGU_KONFIRMASI_STAF")
        void rejectPayment_WrongStatus() {
            testReservasi.setStatus(ReservasiStatus.BELUM_DIBAYAR);
            when(reservasiRepository.findByIdWithLapangan(reservasiId))
                    .thenReturn(Optional.of(testReservasi));

            assertThrows(ConflictException.class,
                    () -> pembayaranService.rejectPayment(reservasiId));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // EXPIRE OVERDUE RESERVATIONS
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Expire Overdue Reservations Tests")
    class ExpireOverdueTests {

        @Test
        @DisplayName("Should expire overdue reservations")
        void expireOverdueReservations_WithExpired() {
            Reservasi expired = Reservasi.builder()
                    .id(UUID.randomUUID())
                    .status(ReservasiStatus.BELUM_DIBAYAR)
                    .lapangan(testCourt)
                    .paymentDeadline(LocalDateTime.now().minusHours(1))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(reservasiRepository.findExpiredReservations(any())).thenReturn(List.of(expired));
            when(reservasiRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            int count = pembayaranService.expireOverdueReservations();

            assertEquals(1, count);
            verify(reservasiRepository).save(argThat(r -> r.getStatus() == ReservasiStatus.EXPIRED));
        }

        @Test
        @DisplayName("Should return zero when no overdue reservations")
        void expireOverdueReservations_None() {
            when(reservasiRepository.findExpiredReservations(any())).thenReturn(Collections.emptyList());

            int count = pembayaranService.expireOverdueReservations();

            assertEquals(0, count);
        }

        @Test
        @DisplayName("Should reset court status to TERSEDIA when not already TERSEDIA")
        void expireOverdue_ResetCourtStatus() {
            testCourt.setStatus(LapanganStatus.DISEWAKAN);
            Reservasi expired = Reservasi.builder()
                    .id(UUID.randomUUID())
                    .status(ReservasiStatus.BELUM_DIBAYAR)
                    .lapangan(testCourt)
                    .paymentDeadline(LocalDateTime.now().minusHours(1))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(reservasiRepository.findExpiredReservations(any())).thenReturn(List.of(expired));
            when(reservasiRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(lapanganRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            pembayaranService.expireOverdueReservations();

            verify(lapanganRepository).save(argThat(l -> l.getStatus() == LapanganStatus.TERSEDIA));
        }
    }
}
