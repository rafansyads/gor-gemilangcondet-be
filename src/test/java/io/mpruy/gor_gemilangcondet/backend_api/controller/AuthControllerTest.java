package io.mpruy.gor_gemilangcondet.backend_api.controller;

import tools.jackson.databind.ObjectMapper;
import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseRequestDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.requests.LoginRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.requests.RefreshTokenRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.requests.RegisterRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.responses.AuthResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.authentications.responses.RegisterResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.users.UserDto;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.RoleName;
import io.mpruy.gor_gemilangcondet.backend_api.service.AuthService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(CacheAutoConfiguration.class)
@TestPropertySource(properties = "spring.cache.type=none")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private JwtTokenFilter jwtTokenFilter;
    @MockitoBean
    private JwtUtils jwtUtils;
    @MockitoBean
    private JwtTokenBlacklist jwtTokenBlacklist;

    @Test
    @DisplayName("POST /auth/login → 200 OK")
    void login_Success() throws Exception {
        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername("testuser");
        loginReq.setPassword("Pass1234");
        BaseRequestDto<LoginRequest> request = new BaseRequestDto<>();
        request.setData(loginReq);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("token").refreshToken("refresh").tokenType("Bearer")
                .username("testuser").role("MEMBER").build();

        when(authService.login(any(), any())).thenReturn(authResponse);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("token"))
                .andExpect(jsonPath("$.message").value("Berhasil login"));
    }

    @Test
    @DisplayName("POST /auth/login-admin → 200 OK")
    void loginAdmin_Success() throws Exception {
        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername("admin");
        loginReq.setPassword("Pass1234");
        BaseRequestDto<LoginRequest> request = new BaseRequestDto<>();
        request.setData(loginReq);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("admin-token").refreshToken("refresh").tokenType("Bearer")
                .username("admin").role("ADMIN").build();

        when(authService.loginAdmin(any(), any())).thenReturn(authResponse);

        mockMvc.perform(post("/auth/login-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("admin-token"));
    }

    @Test
    @DisplayName("POST /auth/register → 201 Created")
    void register_Success() throws Exception {
        RegisterRequest regReq = new RegisterRequest();
        regReq.setUsername("newuser");
        regReq.setEmail("new@test.com");
        regReq.setPassword("Pass1234");
        BaseRequestDto<RegisterRequest> request = new BaseRequestDto<>();
        request.setData(regReq);

        RegisterResponse regResponse = RegisterResponse.builder()
                .user(UserDto.builder().username("newuser").email("new@test.com").role(RoleName.GUEST).build())
                .message("success").build();

        when(authService.register(any())).thenReturn(regResponse);

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.user.username").value("newuser"));
    }

    @Test
    @DisplayName("POST /auth/register-admin → 201 Created")
    void registerAdmin_Success() throws Exception {
        RegisterRequest regReq = new RegisterRequest();
        regReq.setUsername("staffuser");
        regReq.setEmail("staff@test.com");
        regReq.setPassword("Pass1234");
        regReq.setRole("STAF_LAPANGAN");
        BaseRequestDto<RegisterRequest> request = new BaseRequestDto<>();
        request.setData(regReq);

        RegisterResponse regResponse = RegisterResponse.builder()
                .user(UserDto.builder().username("staffuser").email("staff@test.com").role(RoleName.STAF_LAPANGAN)
                        .build())
                .message("success").build();

        when(authService.registerAdmin(any())).thenReturn(regResponse);

        mockMvc.perform(post("/auth/register-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.user.role").value("STAF_LAPANGAN"));
    }

    @Test
    @DisplayName("POST /auth/logout → 200 OK")
    void logout_Success() throws Exception {
        mockMvc.perform(post("/auth/logout")
                .header("Authorization", "Bearer some-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout berhasil"));
    }

    @Test
    @DisplayName("POST /auth/refresh → 200 OK")
    void refresh_Success() throws Exception {
        RefreshTokenRequest refreshReq = new RefreshTokenRequest();
        refreshReq.setRefreshToken("old-refresh");
        BaseRequestDto<RefreshTokenRequest> request = new BaseRequestDto<>();
        request.setData(refreshReq);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("new-token").refreshToken("old-refresh").tokenType("Bearer")
                .username("testuser").role("MEMBER").build();

        when(authService.refreshToken(any())).thenReturn(authResponse);

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-token"));
    }
}
