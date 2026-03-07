package io.mpruy.gor_gemilangcondet.backend_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    private static final String DEV_FRONTEND_ORIGIN = "http://localhost:3000"; // Vue dev server

    // Provide a safe default for local/dev if env var is absent.
    // In production, set CORS_ALLOWED_ORIGINS to a comma-separated list, e.g.
    // https://app.example.com,https://admin.example.com
    @Value("${CORS_ALLOWED_ORIGINS:http://localhost:3000}")
    private String allowedOrigins;

    /**
	 * Parses the allowedOrigins into an array of origin strings.
	 */
	private String[] getOrigins() {
		return allowedOrigins.trim().isEmpty() 
			? new String[] { DEV_FRONTEND_ORIGIN }
			: Arrays.stream(allowedOrigins.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.toArray(String[]::new);
	}

    /**
	 * CorsConfigurationSource bean for Spring Security.
	 * This is used by `.cors(Customizer.withDefaults())` in WebSecurityConfig.
	 * Without this bean, Spring Security won't apply CORS headers to preflight (OPTIONS) requests.
	 */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(List.of(getOrigins()));
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setExposedHeaders(Arrays.asList("Authorization", "X-Forward-Token"));
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L); // cache pre-flight for 1 hour

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

    /**
	 * WebMvcConfigurer for Spring MVC CORS (backup, for non-security filtered requests).
	 */
	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**")
					.allowedOrigins(getOrigins())
					.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
					.allowedHeaders("*")
					.exposedHeaders("Authorization", "X-Forward-Token")
					.allowCredentials(true)
					.maxAge(3600); // cache pre-flight for 1 hour
			}
		};
	}
}
