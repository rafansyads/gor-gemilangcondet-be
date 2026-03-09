package io.mpruy.gor_gemilangcondet.backend_api.dto.fadhil;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Wrapper respons dari backend Fadhil (BaseResponseDto).
 * Semua endpoint Fadhil membungkus data dalam format ini.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FadhilBaseResponse<T> {
    private String status;
    private String message;
    private T data;
}
