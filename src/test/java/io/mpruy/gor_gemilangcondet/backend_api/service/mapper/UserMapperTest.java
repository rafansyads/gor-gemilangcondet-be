package io.mpruy.gor_gemilangcondet.backend_api.service.mapper;

import io.mpruy.gor_gemilangcondet.backend_api.dto.users.UserDto;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.Role;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.RoleName;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    private final UserMapper userMapper = new UserMapper();

    @Test
    @DisplayName("Should map User entity to UserDto")
    void toDto_Success() {
        UUID userId = UUID.randomUUID();
        Role role = new Role();
        role.setId(1);
        role.setRoleName(RoleName.MEMBER);
        User user = User.builder()
                .id(userId).username("testuser")
                .email("test@test.com").password("Pass1234").role(role)
                .build();

        UserDto result = userMapper.toDto(user);

        assertEquals(userId, result.getId());
        assertEquals("testuser", result.getUsername());
        assertEquals("test@test.com", result.getEmail());
        assertEquals(RoleName.MEMBER, result.getRole());
    }
}
