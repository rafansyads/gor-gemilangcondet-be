package io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.responses;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String token;
    @Builder.Default
    private String type = "Bearer";
    private String id;
    private String username;
    private String email;
    private String role;
    private String status;
}
