package io.mpruy.gor_gemilangcondet.backend_api.dto;

import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO standar untuk permintaan REST API yang memerlukan timestamp.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class BaseRequestDto<T> {
    private Integer status;
    private String message;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Asia/Jakarta")
    private Instant timestamp;
    private T data;
}

