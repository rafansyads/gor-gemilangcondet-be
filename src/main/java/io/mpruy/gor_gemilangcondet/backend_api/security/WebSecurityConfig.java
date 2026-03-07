package io.mpruy.gor_gemilangcondet.backend_api.security;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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

    private final JwtTokenFilter jwtTokenFilter;
    private final AuthenticationProvider authenticationProvider;

    /**
     * Main security filter chain.
     * <p>
     * Current policy: all endpoints ({@code /**}) are permitted without authentication.
     * RBAC rules will be added incrementally once controllers are implemented.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/**")
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(requests -> requests
                // Always allow preflight requests
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Auth controller: allow all (login and register are public)
                .requestMatchers("/auth/**").permitAll()
                
                // TODO: narrow down once RBAC is defined per endpoint
                .requestMatchers("/**").permitAll() // temporary, to be replaced with actual RBAC rules

                // // User controller: limit GET /users and /users/by-username/** to ADMIN, STAF_LAPANGAN, and STAF_TOKO
                // // GET /users/{id} can be accessed by the user themselves or by staff/admin
                // .requestMatchers(HttpMethod.GET, "/users", "/users/by-username/**")
                //     .hasAnyAuthority("ADMIN", "STAF_LAPANGAN", "STAF_TOKO")
                // .requestMatchers(HttpMethod.GET, "/users/**")// custom logic in controller to check if user is accessing their own data or is staff/admin
                //     .hasAnyAuthority("ADMIN", "STAF_LAPANGAN", "STAF_TOKO", "MEMBER", "GUEST", "OWNER")
            )
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
                        response.getWriter().write("Forbidden");
                    }
                })
            );

        return http.build();
    }
}
