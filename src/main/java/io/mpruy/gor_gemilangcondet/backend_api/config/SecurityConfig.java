package io.mpruy.gor_gemilangcondet.backend_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Konfigurasi Spring Security untuk GOR Gemilang Condet Backend.
 *
 * <p>Endpoint jadwal ({@code /api/schedule/**}) dan WebSocket ({@code /ws/**})
 * dibiarkan <strong>publik</strong> agar pelanggan dapat melihat ketersediaan
 * lapangan tanpa harus login terlebih dahulu.
 *
 * <p>Endpoint lain (manajemen booking, dll.) akan dikonfigurasi lebih lanjut
 * seiring penambahan fitur berikutnya.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Nonaktifkan CSRF — API stateless (JWT), tidak membutuhkan CSRF token
            .csrf(AbstractHttpConfigurer::disable)

            // Izinkan H2 console di-render dalam iframe (dev only)
            .headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))

            // Endpoint API stateless pakai JWT — tidak butuh HTTP session.
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

            .authorizeHttpRequests(auth -> auth
                // ── Publik ──────────────────────────────────────────────────
                .requestMatchers("/api/schedule/**").permitAll()
                // Endpoint test simulasi booking (dev only)
                .requestMatchers("/api/test/**").permitAll()
                // Static files — halaman test interface
                .requestMatchers("/", "/test-schedule.html", "/*.html", "/**.html").permitAll()
                // H2 console (dev only)
                .requestMatchers("/h2-console/**").permitAll()
                // WebSocket STOMP endpoint + semua path SockJS
                .requestMatchers("/ws/**").permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
