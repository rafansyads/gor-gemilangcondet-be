package io.mpruy.gor_gemilangcondet.backend_api.entities.users;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatus {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String name;
}
