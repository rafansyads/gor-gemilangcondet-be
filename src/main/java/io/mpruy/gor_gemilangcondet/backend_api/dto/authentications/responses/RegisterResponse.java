package io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.responses;

import io.mpruy.gor_gemilangcondet.backend_api.dto.users.UserDto;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisterResponse {
    private UserDto user;
    private String message;
}
