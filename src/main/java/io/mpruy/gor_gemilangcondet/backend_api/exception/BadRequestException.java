package io.mpruy.gor_gemilangcondet.backend_api.exception;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
