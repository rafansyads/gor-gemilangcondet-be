package io.mpruy.gor_gemilangcondet.backend_api.exception;

/**
 * Dilempar ketika layanan eksternal (misalnya backend Fadhil) tidak dapat
 * dijangkau atau mengembalikan respons yang tidak terduga sehingga operasi
 * tidak dapat diselesaikan.
 */
public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
