package io.mpruy.gor_gemilangcondet.backend_api.entities.reservations;

public enum ReservasiStatus {
    BELUM_DIBAYAR,
    MENUNGGU_KONFIRMASI_STAF,
    DIKONFIRMASI,
    DITOLAK,
    EXPIRED,
    DOWN_PAYMENT,
    DIBAYAR,
    DIBATALKAN,
    SELESAI
}
