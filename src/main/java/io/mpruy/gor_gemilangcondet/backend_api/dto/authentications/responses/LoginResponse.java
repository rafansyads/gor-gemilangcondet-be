package io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String token;
    private String type;
    private Long id;
    private String username;
    private String email;
    private String role;
    private String status;
}
