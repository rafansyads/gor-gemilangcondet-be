package io.mpruy.gor_gemilangcondet.backend_api.entities.users;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    private static final ZoneId ZONE_JAKARTA = ZoneId.of("Asia/Jakarta");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    private LocalDateTime membershipStart;

    private LocalDateTime membershipEnd;

    @Column(nullable = false)
    private String password;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "status_id", nullable = false)
    private UserStatus status;

    /** Timestamp of when account was marked BANNED; null otherwise. */
    private LocalDateTime bannedAt;

    @PrePersist
    @PreUpdate
    private void syncBannedTimestamp() {
        if (status != null && status.getName() == UserStatusName.BANNED) {
            if (bannedAt == null) {
                bannedAt = LocalDateTime.now(ZONE_JAKARTA);
            }
            return;
        }
        bannedAt = null;
    }
}
