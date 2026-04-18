package io.mpruy.gor_gemilangcondet.backend_api.exception;

public class DownloadContentException extends RuntimeException {
    public DownloadContentException(String message) {
        super(message);
    }

    public DownloadContentException(String message, Throwable cause) {
        super(message, cause);
    }
}
