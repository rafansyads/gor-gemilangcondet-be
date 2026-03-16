package io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Username atau email wajib diisi")
    private String usernameOrEmail;

    @NotBlank(message = "Password wajib diisi")
    private String password;
}
