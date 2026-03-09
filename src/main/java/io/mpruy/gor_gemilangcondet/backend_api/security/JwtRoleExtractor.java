package io.mpruy.gor_gemilangcondet.backend_api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;

/**
 * Komponen ringan untuk mengekstrak klaim {@code role} dari JWT Fadhil.
 *
 * <p>Tidak memvalidasi masa berlaku token — hanya membaca klaim {@code role}
 * agar endpoint jadwal dapat menentukan tingkat detail yang ditampilkan
 * (ADMIN/OWNER/STAF_LAPANGAN melihat {@code namaWakil}, yang lain tidak).
 *
 * <p>Jika header Authorization tidak ada atau token tidak valid, mengembalikan
 * {@code null} yang diperlakukan sebagai pengguna anonim (GUEST).
 */
@Slf4j
@Component
public class JwtRoleExtractor {

    private final Key signingKey;

    public JwtRoleExtractor(@Value("${app.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Ekstrak nilai klaim {@code role} dari header Authorization.
     *
     * @param authorizationHeader nilai header Authorization, bisa null
     * @return nama role (misal {@code "ADMIN"}) atau {@code null} jika tidak ada / tidak valid
     */
    public String extractRole(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authorizationHeader.substring(7);
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("role", String.class);
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("[JWT] Token tidak valid, dianggap anonim: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Cek apakah role berhak melihat {@code namaWakil} (data pribadi pemesan).
     *
     * @param role nilai role dari token, bisa null
     * @return {@code true} jika ADMIN, OWNER, atau STAF_LAPANGAN
     */
    public boolean canViewBookerName(String role) {
        if (role == null) return false;
        return switch (role.toUpperCase()) {
            case "ADMIN", "OWNER", "STAF_LAPANGAN" -> true;
            default -> false;
        };
    }
}
