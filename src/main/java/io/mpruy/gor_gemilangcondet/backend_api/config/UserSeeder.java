package io.mpruy.gor_gemilangcondet.backend_api.config;

import io.mpruy.gor_gemilangcondet.backend_api.entities.users.*;
import io.mpruy.gor_gemilangcondet.backend_api.repository.RoleRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserStatusRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class UserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserStatusRepository userStatusRepository;
    private final PasswordEncoder passwordEncoder;

    public UserSeeder(UserRepository userRepository,
                      RoleRepository roleRepository,
                      UserStatusRepository userStatusRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userStatusRepository = userStatusRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedInitialAdmin();
    }

    private void seedInitialAdmin() {
        if (userRepository.existsByUsername("admin")) {
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("Role ROLE_ADMIN tidak ditemukan"));

        UserStatus aktifStatus = userStatusRepository.findByName(UserStatusName.AKTIF)
                .orElseThrow(() -> new IllegalStateException("Status AKTIF tidak ditemukan"));

        User admin = User.builder()
                .nama("Administrator")
                .email("admin@gor-gemilangcondet.id")
                .username("admin")
                .password(passwordEncoder.encode("Admin@1234"))
                .role(adminRole)
                .status(aktifStatus)
                .build();

        userRepository.save(admin);
    }
}
