package io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @Email(message = "Email must be valid")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    // Optional: this is to differentiate between user (default, null)
    // or staffs (staf lapangan / staf toko) given from admin frontend
    private String role;
}
