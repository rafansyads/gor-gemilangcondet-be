package io.mpruy.gor_gemilangcondet.backend_api.config;

import io.mpruy.gor_gemilangcondet.backend_api.entities.roles.RoleName;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.User;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatusName;
import io.mpruy.gor_gemilangcondet.backend_api.repository.RoleRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class UserSeeder implements CommandLineRunner {

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserStatusRepository userStatusRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.existsByUsername("admin")) {
            return;
        }
        var adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElse(null);
        var aktifStatus = userStatusRepository.findByName(UserStatusName.AKTIF).orElse(null);
        if (adminRole == null || aktifStatus == null) {
            return;
        }
        User admin = User.builder()
                .username("admin")
                .email("admin@gor-gemilangcondet.id")
                .password(passwordEncoder.encode("Admin@1234!"))
                .role(adminRole)
                .status(aktifStatus)
                .build();
        userRepository.save(admin);
    }
}
