package io.mpruy.gor_gemilangcondet.backend_api.dto.users;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private String id;
    private String username;
    private String email;
    private String role;
    private String status;
    private LocalDateTime createdAt;
}
