package io.mpruy.gor_gemilangcondet.backend_api.service.mapper;

import org.springframework.stereotype.Component;

import io.mpruy.gor_gemilangcondet.backend_api.dto.users.UserDto;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.User;

@Component
public class UserMapper {

    public UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().getRoleName())
                .membershipStart(user.getMembershipStart())
                .membershipEnd(user.getMembershipEnd())
                .build();
    }
}
