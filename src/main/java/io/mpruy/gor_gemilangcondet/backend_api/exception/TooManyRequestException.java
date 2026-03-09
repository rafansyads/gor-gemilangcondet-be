package io.mpruy.gor_gemilangcondet.backend_api.exception;

public class TooManyRequestException extends RuntimeException {
    public TooManyRequestException(String message) {
        super(message);
    }    
}
