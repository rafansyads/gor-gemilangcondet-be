package io.mpruy.gor_gemilangcondet.backend_api.security;

import io.mpruy.gor_gemilangcondet.backend_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        return userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .map(UserDetailsImpl::new)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Pengguna tidak ditemukan: " + usernameOrEmail));
    }

    
}
