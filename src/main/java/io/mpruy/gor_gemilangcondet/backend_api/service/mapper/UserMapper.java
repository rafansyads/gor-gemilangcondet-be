package io.mpruy.gor_gemilangcondet.backend_api.service.mapper;

import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.responses.RegisterResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.users.UserDto;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public RegisterResponse toRegisterResponse(User user) {
        return RegisterResponse.builder()
                .id(user.getId())
                .nama(user.getNama())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole().getName().name())
                .status(user.getStatus().getName().name())
                .build();
    }

    public UserDto toUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .nama(user.getNama())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole().getName().name())
                .status(user.getStatus().getName().name())
                .build();
    }
}
