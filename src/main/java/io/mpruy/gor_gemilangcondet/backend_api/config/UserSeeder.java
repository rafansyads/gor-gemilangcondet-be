package io.mpruy.gor_gemilangcondet.backend_api.config;

import io.mpruy.gor_gemilangcondet.backend_api.entities.users.Role;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.RoleName;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.User;
import io.mpruy.gor_gemilangcondet.backend_api.repository.RoleRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seeds 18 hardcoded dummy users (3 per role) into the database on startup.
 * Only active in the {@code dev} profile. Idempotent — skips users whose
 * email already exists.
 *
 * All passwords: password123
 */
@Component
@Profile("dev")
@Order(2) // run after DataSeeder (which seeds roles)
@RequiredArgsConstructor
@Slf4j
public class UserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private record DummyUser(String username, String email, String password, RoleName role) {
    }

    /** All dummy users — password is always {@code password123} for easy login. */
    private static final List<DummyUser> DUMMY_USERS = List.of(
            // ── GUEST ──
            new DummyUser("guest1", "guest1@example.com", "password123", RoleName.GUEST),
            new DummyUser("guest2", "guest2@example.com", "password123", RoleName.GUEST),
            new DummyUser("guest3", "guest3@example.com", "password123", RoleName.GUEST),

            // ── MEMBER ──
            new DummyUser("member1", "member1@example.com", "password123", RoleName.MEMBER),
            new DummyUser("member2", "member2@example.com", "password123", RoleName.MEMBER),
            new DummyUser("member3", "member3@example.com", "password123", RoleName.MEMBER),

            // ── STAF LAPANGAN ──
            new DummyUser("staf_lapangan1", "staf.lapangan1@example.com", "password123", RoleName.STAF_LAPANGAN),
            new DummyUser("staf_lapangan2", "staf.lapangan2@example.com", "password123", RoleName.STAF_LAPANGAN),
            new DummyUser("staf_lapangan3", "staf.lapangan3@example.com", "password123", RoleName.STAF_LAPANGAN),

            // ── STAF TOKO ──
            new DummyUser("staf_toko1", "staf.toko1@example.com", "password123", RoleName.STAF_TOKO),
            new DummyUser("staf_toko2", "staf.toko2@example.com", "password123", RoleName.STAF_TOKO),
            new DummyUser("staf_toko3", "staf.toko3@example.com", "password123", RoleName.STAF_TOKO),

            // ── OWNER ──
            new DummyUser("owner1", "owner1@example.com", "password123", RoleName.OWNER),
            new DummyUser("owner2", "owner2@example.com", "password123", RoleName.OWNER),
            new DummyUser("owner3", "owner3@example.com", "password123", RoleName.OWNER),

            // ── ADMIN ──
            new DummyUser("admin1", "admin1@example.com", "password123", RoleName.ADMIN),
            new DummyUser("admin2", "admin2@example.com", "password123", RoleName.ADMIN),
            new DummyUser("admin3", "admin3@example.com", "password123", RoleName.ADMIN));

    @Override
    @Transactional
    public void run(String... args) {
        log.info("=== [DEV] Starting dummy user seeding ===");

        int created = 0;
        for (DummyUser dummy : DUMMY_USERS) {
            if (userRepository.existsByEmail(dummy.email())) {
                log.debug("User already exists, skipping: {}", dummy.email());
                continue;
            }

            Role role = roleRepository.findByRoleName(dummy.role())
                    .orElseThrow(() -> new IllegalStateException(
                            "Role not found: " + dummy.role() + ". Ensure DataSeeder runs first."));

            User user = User.builder()
                    .username(dummy.username())
                    .email(dummy.email())
                    .password(passwordEncoder.encode(dummy.password()))
                    .role(role)
                    .build();

            userRepository.save(user);
            created++;
            log.info("Seeded user: {} | email: {} | role: {} | password: {}",
                    dummy.username(), dummy.email(), dummy.role(), dummy.password());
        }

        log.info("=== [DEV] User seeding complete — {} new user(s) created ===", created);
    }
}
