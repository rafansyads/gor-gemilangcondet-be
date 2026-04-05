package io.mpruy.gor_gemilangcondet.backend_api.security;

import io.mpruy.gor_gemilangcondet.backend_api.entities.users.User;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatusName;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserDetailsImpl implements UserDetails {

    private final String id;
    private final String username;
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final UserStatusName status;

    public UserDetailsImpl(String id, String username, String email, String password,
                           Collection<? extends GrantedAuthority> authorities, UserStatusName status) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
        this.status = status;
    }

    public static UserDetailsImpl build(User user) {
        List<GrantedAuthority> auths = List.of(
                new SimpleGrantedAuthority(user.getRole().getName().name()));
        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                auths,
                user.getStatus().getName());
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public UserStatusName getStatus() { return status; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        return status == UserStatusName.AKTIF;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatusName.SUSPENDED && status != UserStatusName.BANNED;
    }
}
