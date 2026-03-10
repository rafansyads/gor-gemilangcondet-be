package io.mpruy.gor_gemilangcondet.backend_api.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Representasi satu lapangan badminton di GOR Gemilang Condet.
 * GOR ini memiliki 6 lapangan (id 1–6).
 */
@Entity
@Table(name = "courts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Court {

    @Id
    @Column(nullable = false)
    private Integer id;

    /** Nama tampilan, mis. "Court 1" s/d "Court 6" */
    @Column(nullable = false, length = 50)
    private String name;
}
