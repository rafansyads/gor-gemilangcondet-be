package io.mpruy.gor_gemilangcondet.backend_api.config;

import io.mpruy.gor_gemilangcondet.backend_api.entities.users.Role;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.RoleName;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatusName;
import io.mpruy.gor_gemilangcondet.backend_api.repository.RoleRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserStatusRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private final UserStatusRepository userStatusRepository;
    private final RoleRepository roleRepository;

    public DataSeeder(UserStatusRepository userStatusRepository, RoleRepository roleRepository) {
        this.userStatusRepository = userStatusRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        seedUserStatuses();
        seedRoles();
    }

    private void seedUserStatuses() {
        for (UserStatusName statusName : UserStatusName.values()) {
            if (userStatusRepository.findByName(statusName).isEmpty()) {
                userStatusRepository.save(UserStatus.builder().name(statusName).build());
            }
        }
    }

    private void seedRoles() {
        for (RoleName roleName : RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(Role.builder().name(roleName).build());
            }
        }
    }
}
