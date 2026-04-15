package io.mpruy.gor_gemilangcondet.backend_api.integration;

import io.mpruy.gor_gemilangcondet.backend_api.entities.users.Role;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.RoleName;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.User;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatusName;
import io.mpruy.gor_gemilangcondet.backend_api.repository.RoleRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserStatusRepository;
import io.mpruy.gor_gemilangcondet.backend_api.security.jwt.JwtUtils;
import io.mpruy.gor_gemilangcondet.backend_api.security.service.RefreshTokenService;
import io.mpruy.gor_gemilangcondet.backend_api.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DataJpaTest
@Import(UserService.class)
@ImportAutoConfiguration(CacheAutoConfiguration.class)
@TestPropertySource(properties = "spring.cache.type=none")
class UserBannedCleanupIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserStatusRepository userStatusRepository;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("deleteExpiredBannedUsers should delete only users banned for at least 3 days")
    void deleteExpiredBannedUsers_DeletesOnlyExpiredBannedUsers() {
        Role staffRole = roleRepository.findByRoleName(RoleName.STAF_LAPANGAN)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(RoleName.STAF_LAPANGAN).build()));

        UserStatus bannedStatus = userStatusRepository.findByName(UserStatusName.BANNED)
                .orElseGet(() -> userStatusRepository.save(UserStatus.builder().name(UserStatusName.BANNED).build()));

        UserStatus activeStatus = userStatusRepository.findByName(UserStatusName.AKTIF)
                .orElseGet(() -> userStatusRepository.save(UserStatus.builder().name(UserStatusName.AKTIF).build()));

        User expiredBanned = userRepository.save(User.builder()
                .username("expired_banned")
                .email("expired_banned@test.com")
                .password("encoded")
                .role(staffRole)
                .status(bannedStatus)
                .bannedAt(LocalDateTime.now().minusDays(4))
                .build());

        User recentBanned = userRepository.save(User.builder()
                .username("recent_banned")
                .email("recent_banned@test.com")
                .password("encoded")
                .role(staffRole)
                .status(bannedStatus)
                .bannedAt(LocalDateTime.now().minusHours(12))
                .build());

        User activeUser = userRepository.save(User.builder()
                .username("active_user")
                .email("active_user@test.com")
                .password("encoded")
                .role(staffRole)
                .status(activeStatus)
                .build());

        int deletedCount = userService.deleteExpiredBannedUsers();

        assertEquals(1, deletedCount);
        assertFalse(userRepository.existsById(expiredBanned.getId()));
        assertTrue(userRepository.existsById(recentBanned.getId()));
        assertTrue(userRepository.existsById(activeUser.getId()));
        verify(refreshTokenService, times(1)).deleteRefreshTokenByUsername("expired_banned");
    }
}
