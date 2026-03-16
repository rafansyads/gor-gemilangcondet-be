package io.mpruy.gor_gemilangcondet.backend_api.controller;

import tools.jackson.databind.ObjectMapper;
import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseRequestDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.users.UserDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.users.requests.UpdateProfileRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.users.responses.UpdateProfileResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.RoleName;
import io.mpruy.gor_gemilangcondet.backend_api.service.UserService;
import io.mpruy.gor_gemilangcondet.backend_api.security.jwt.JwtTokenFilter;
import io.mpruy.gor_gemilangcondet.backend_api.security.jwt.JwtUtils;
import io.mpruy.gor_gemilangcondet.backend_api.security.service.JwtTokenBlacklist;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(CacheAutoConfiguration.class)
@TestPropertySource(properties = "spring.cache.type=none")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;
    @MockitoBean
    private JwtTokenFilter jwtTokenFilter;
    @MockitoBean
    private JwtUtils jwtUtils;
    @MockitoBean
    private JwtTokenBlacklist jwtTokenBlacklist;

    private UserDto buildUserDto() {
        return UserDto.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@test.com")
                .role(RoleName.MEMBER)
                .build();
    }

    @Test
    @DisplayName("GET /users → 200 OK with user list")
    void getAllUsers_Success() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(buildUserDto()));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].username").value("testuser"));
    }

    @Test
    @DisplayName("GET /users/{id} → 200 OK")
    void getUserById_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        UserDto dto = buildUserDto();
        when(userService.getUserById(userId)).thenReturn(dto);

        mockMvc.perform(get("/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    @DisplayName("GET /users/by-username/{username} → 200 OK")
    void getUserByUsername_Success() throws Exception {
        when(userService.getUserByUsername("testuser")).thenReturn(buildUserDto());

        mockMvc.perform(get("/users/by-username/testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    @DisplayName("PUT /users/profile → 200 OK")
    void updateProfile_Success() throws Exception {
        UpdateProfileRequest updateReq = new UpdateProfileRequest();
        updateReq.setUsername("updated");
        updateReq.setEmail("updated@test.com");

        BaseRequestDto<UpdateProfileRequest> request = new BaseRequestDto<>();
        request.setData(updateReq);

        UserDto updatedUser = UserDto.builder()
                .id(UUID.randomUUID())
                .username("updated")
                .email("updated@test.com")
                .role(RoleName.MEMBER)
                .build();
        UpdateProfileResponse updated = UpdateProfileResponse.builder()
                .user(updatedUser)
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .build();

        when(userService.updateProfile(any(UpdateProfileRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/users/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.username").value("updated"));
    }
}
