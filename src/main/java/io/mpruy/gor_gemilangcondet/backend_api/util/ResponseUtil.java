package io.mpruy.gor_gemilangcondet.backend_api.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseResponseDto;

import java.time.Instant;

/**
 * Utility to build standardized REST responses using
 * (props to practice from)
 * {@link io.mpruy.gor_gemilangcondet.backend_api.dto.BaseResponseDto}.
 * Wrap all REST controller responses with this to keep the format consistent.
 */
@Component
public class ResponseUtil {

    /**
     * A small wrapper around ResponseEntity that exposes a toBuilder() API
     * which preserves the original body while allowing headers/status changes.
     * This enables usage like:
     * return ResponseUtil.success(body, "...", HttpStatus.OK)
     * .toBuilder().headers(headers).build();
     */
    public static class WithBuilder<T> {
        private final ResponseEntity<T> entity;

        public WithBuilder(ResponseEntity<T> entity) {
            this.entity = entity;
        }

        public Builder toBuilder() {
            return new Builder();
        }

        public class Builder {
            private final org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();

            private Builder() {
                // start from original headers
                this.headers.putAll(entity.getHeaders());
            }

            public Builder headers(org.springframework.http.HttpHeaders headers) {
                if (headers != null) {
                    this.headers.putAll(headers);
                }
                return this;
            }

            public ResponseEntity<T> build() {
                return new ResponseEntity<>(entity.getBody(), this.headers, entity.getStatusCode());
            }
        }
    }

    /**
     * Build a success response with payload.
     * 
     * @param data    domain/DTO payload to return
     * @param message human friendly success message
     * @param status  HTTP status to send (e.g., 200, 201)
     */
    public static <T> WithBuilder<BaseResponseDto<T>> success(T data, String message, HttpStatus status) {
        BaseResponseDto<T> response = new BaseResponseDto<>();
        response.setStatus(status.value());
        response.setMessage(message);
        response.setData(data);
        response.setTimestamp(Instant.now());
        ResponseEntity<BaseResponseDto<T>> entity = new ResponseEntity<>(response, status);
        return new WithBuilder<>(entity);
    }

    /**
     * Build an error response without payload.
     * 
     * @param message error details suitable for clients
     * @param status  HTTP error status (e.g., 400, 404, 409, 500)
     */
    public static <T> ResponseEntity<BaseResponseDto<T>> error(String message, HttpStatus status) {
        BaseResponseDto<T> response = new BaseResponseDto<>();
        response.setStatus(status.value());
        response.setMessage(message);
        response.setData(null);
        response.setTimestamp(Instant.now());
        return new ResponseEntity<>(response, status);
    }
}