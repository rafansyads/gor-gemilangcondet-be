package io.mpruy.gor_gemilangcondet.backend_api.entities.users;

/**
 * Fixed set of user status names on the system.
 * Status digunakan untuk menentukan apakah user masih pending, 
 *        sudah aktif, atau sudah tidak aktif lagi.
 * 
 * @PENDING: use case utama untuk STAF_LAPANGAN, STAF_TOKO,
 *        ADMIN, & OWNER yang menggunakan fitur registrasi. 
 *        Status ini menandakan bahwa user sudah mendaftar, 
 *        tapi belum diverifikasi oleh admin.
 * 
 * @AKTIF: user yang telah diverifikasi dan sedang aktif 
 *        menggunakan layanan.
 * 
 * @NON_AKTIF: user yang telah terdaftar tetapi tidak lagi 
 *        menggunakan layanan.
 * 
 * @SUSPENDED: user yang telah disuspensi oleh admin.
 * 
 * @BANNED: user yang telah dilarang dari menggunakan layanan.
 */
public enum UserStatusName {
    PENDING,
    AKTIF,
    NON_AKTIF,
    SUSPENDED,
    BANNED
}