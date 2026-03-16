package io.mpruy.gor_gemilangcondet.backend_api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO standar untuk seluruh respons REST API.
 * Membungkus data dengan status HTTP, pesan, dan waktu respons.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class BaseResponseDto<T> {
    private int status;
    private String message;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Asia/Jakarta")
    private Instant timestamp;
    private T data;
}
