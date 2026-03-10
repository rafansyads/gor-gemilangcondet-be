package io.mpruy.gor_gemilangcondet.backend_api.enums;

/**
 * Status siklus hidup sebuah booking lapangan.
 * PENDING   : Booking dibuat, menunggu konfirmasi / pembayaran.
 * CONFIRMED : Pembayaran diterima, slot TERKUNCI.
 * CANCELLED : Booking dibatalkan, slot kembali AVAILABLE.
 */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
