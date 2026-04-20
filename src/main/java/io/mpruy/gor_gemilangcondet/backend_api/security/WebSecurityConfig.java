package io.mpruy.gor_gemilangcondet.backend_api.security;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;

import io.mpruy.gor_gemilangcondet.backend_api.security.ratelimit.IpRateLimitingFilter;
import io.mpruy.gor_gemilangcondet.backend_api.security.jwt.JwtTokenFilter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final ObjectProvider<IpRateLimitingFilter> ipRateLimitingFilterProvider;
    private final JwtTokenFilter jwtTokenFilter;
    private final AuthenticationProvider authenticationProvider;

    /**
     * Main security filter chain.
     * <p>
     * Public routes are explicitly permitted, while all remaining routes require
     * authentication. Fine-grained role checks are enforced via method security.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/**")
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .authorizeHttpRequests(requests -> requests
                        // Always allow preflight requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Authentication routes -- publik
                        .requestMatchers("/auth/**", "/api/auth/**").permitAll()

                        // Schedule & WebSocket — publik
                        .requestMatchers("/schedule/**", "/api/schedule/**").permitAll()
                        .requestMatchers("/test/**", "/api/test/**").permitAll()
                        .requestMatchers("/ws/**", "/api/ws/**").permitAll()

                        // Court images & payment proof images — publik (no auth needed)
                        .requestMatchers(HttpMethod.GET, "/payments/proof/**", "/api/payments/proof/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/courts/image/**", "/api/courts/image/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/products/**", "/api/products/**").permitAll()

                        // Barang / Produk API
                        .requestMatchers("/barang/**", "/api/barang/**").permitAll()

                        // Static files & H2 console (dev)
                        .requestMatchers("/", "/api", "/test-schedule.html", "/api/test-schedule.html", "/*.html",
                                "/**.html")
                        .permitAll()
                        .requestMatchers("/h2-console/**", "/api/h2-console/**").permitAll()

                        .anyRequest().authenticated())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler(new AccessDeniedHandler() {
                            @Override
                            public void handle(HttpServletRequest request,
                                    HttpServletResponse response,
                                    org.springframework.security.access.AccessDeniedException ex)
                                    throws IOException, ServletException {
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                response.getWriter().write("Akses ditolak");
                            }
                        }));

        IpRateLimitingFilter ipRateLimitingFilter = ipRateLimitingFilterProvider.getIfAvailable();
        if (ipRateLimitingFilter != null) {
            http.addFilterAfter(ipRateLimitingFilter, SecurityContextHolderFilter.class);
        }

        return http.build();
    }
}
