package io.mpruy.gor_gemilangcondet.backend_api.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.mpruy.gor_gemilangcondet.backend_api.dto.users.UserDto;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.RoleName;
import io.mpruy.gor_gemilangcondet.backend_api.security.jwt.JwtTokenFilter;
import io.mpruy.gor_gemilangcondet.backend_api.security.jwt.JwtUtils;
import io.mpruy.gor_gemilangcondet.backend_api.security.service.JwtTokenBlacklist;
import io.mpruy.gor_gemilangcondet.backend_api.service.UserService;
import io.mpruy.gor_gemilangcondet.backend_api.controller.UserController;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc
@Import(WebSecurityConfig.class)
@ImportAutoConfiguration(CacheAutoConfiguration.class)
@TestPropertySource(properties = "spring.cache.type=none")
class UserPendingRegistrationSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenFilter jwtTokenFilter;
    @MockitoBean
    private JwtUtils jwtUtils;
    @MockitoBean
    private JwtTokenBlacklist jwtTokenBlacklist;
    @MockitoBean
    private AuthenticationProvider authenticationProvider;
    @MockitoBean
    private UserService userService;

    @BeforeEach
    void setUpFilterPassThrough() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtTokenFilter).doFilter(any(), any(), any());

        when(jwtUtils.extractUsername(anyString())).thenReturn(null);
    }

    @Test
    @DisplayName("Pending list should return 401 for unauthenticated users")
    void pendingList_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/users/pending-admin-registrations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "MEMBER")
    @DisplayName("Pending list should return 403 for non-admin users")
    void pendingList_NonAdmin_Returns403() throws Exception {
        mockMvc.perform(get("/users/pending-admin-registrations"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("Pending list should return 200 for admin users")
    void pendingList_Admin_Returns200() throws Exception {
        UserDto pending = UserDto.builder()
                .id(UUID.randomUUID())
                .username("pending_admin")
                .email("pending_admin@test.com")
                .role(RoleName.ADMIN)
                .status("PENDING")
                .build();

        when(userService.getPendingAdminRegistrations()).thenReturn(List.of(pending));

        mockMvc.perform(get("/users/pending-admin-registrations"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "MEMBER")
    @DisplayName("Approve pending should return 403 for non-admin users")
    void approvePending_NonAdmin_Returns403() throws Exception {
        mockMvc.perform(patch("/users/pending-admin-registrations/{id}/approve", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("Approve pending should return 200 for admin users")
    void approvePending_Admin_Returns200() throws Exception {
        UUID id = UUID.randomUUID();
        UserDto approved = UserDto.builder()
                .id(id)
                .username("approved_admin")
                .email("approved_admin@test.com")
                .role(RoleName.ADMIN)
                .status("AKTIF")
                .build();

        when(userService.approvePendingAdminRegistration(id)).thenReturn(approved);

        mockMvc.perform(patch("/users/pending-admin-registrations/{id}/approve", id))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "MEMBER")
    @DisplayName("Reject pending should return 403 for non-admin users")
    void rejectPending_NonAdmin_Returns403() throws Exception {
        mockMvc.perform(delete("/users/pending-admin-registrations/{id}/reject", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("Reject pending should return 200 for admin users")
    void rejectPending_Admin_Returns200() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(userService).rejectPendingAdminRegistration(any(UUID.class));

        mockMvc.perform(delete("/users/pending-admin-registrations/{id}/reject", id))
                .andExpect(status().isOk());
    }
}
