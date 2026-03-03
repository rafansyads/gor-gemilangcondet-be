package io.mpruy.gor_gemilangcondet.backend_api.dto.users;

import io.mpruy.gor_gemilangcondet.backend_api.entities.users.RoleName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserDto {

    private UUID id;
    private String username;
    private String email;
    private RoleName role;
    private LocalDateTime membershipStart;
    private LocalDateTime membershipEnd;
}
