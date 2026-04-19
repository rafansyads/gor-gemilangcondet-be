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
                .status(user.getStatus() != null ? user.getStatus().getName().name() : null)
                .membershipStart(user.getMembershipStart())
                .membershipEnd(user.getMembershipEnd())
                .lastLoginAt(user.getLastLoginAt())
                .lastLogoutAt(user.getLastLogoutAt())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .failedLoginWindowStartedAt(user.getFailedLoginWindowStartedAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .statusChangedAt(user.getStatusChangedAt())
                .bannedAt(user.getBannedAt())
                .build();
    }

    public User toEntity(UserDto userDto) {
        return User.builder()
                .id(userDto.getId())
                .username(userDto.getUsername())
                .email(userDto.getEmail())
                .build();
    }
}
