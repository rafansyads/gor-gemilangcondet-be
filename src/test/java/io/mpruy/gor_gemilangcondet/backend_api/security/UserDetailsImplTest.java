package io.mpruy.gor_gemilangcondet.backend_api.security;

import io.mpruy.gor_gemilangcondet.backend_api.entities.users.Role;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.RoleName;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.User;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatusName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDetailsImplTest {

    private User buildUser(RoleName roleName, UserStatusName statusName) {
        Role role = Role.builder().roleName(roleName).build();
        UserStatus status = statusName == null ? null : UserStatus.builder().name(statusName).build();

        return User.builder()
                .id(UUID.randomUUID())
                .username("tester")
                .email("tester@example.com")
                .password("secret")
                .role(role)
                .status(status)
                .build();
    }

    @Test
    @DisplayName("should expose basic user fields and authority")
    void basicFieldsAndAuthority() {
        UserDetailsImpl details = new UserDetailsImpl(buildUser(RoleName.ADMIN, UserStatusName.AKTIF));

        assertEquals("tester", details.getUsername());
        assertEquals("secret", details.getPassword());
        assertEquals(1, details.getAuthorities().size());
        assertEquals("ADMIN", details.getAuthorities().iterator().next().getAuthority());
        assertEquals("AKTIF", details.getStatus());
        assertEquals(details.getUser().getId(), details.getId());
        assertTrue(details.isAccountNonExpired());
        assertTrue(details.isAccountNonLocked());
        assertTrue(details.isCredentialsNonExpired());
    }

    @Test
    @DisplayName("isEnabled should be true only for AKTIF status")
    void isEnabled_ByStatus() {
        UserDetailsImpl aktif = new UserDetailsImpl(buildUser(RoleName.MEMBER, UserStatusName.AKTIF));
        UserDetailsImpl banned = new UserDetailsImpl(buildUser(RoleName.MEMBER, UserStatusName.BANNED));
        UserDetailsImpl noStatus = new UserDetailsImpl(buildUser(RoleName.MEMBER, null));

        assertTrue(aktif.isEnabled());
        assertFalse(banned.isEnabled());
        assertFalse(noStatus.isEnabled());
        assertEquals("BANNED", banned.getStatus());
        assertEquals(null, noStatus.getStatus());
    }
}
