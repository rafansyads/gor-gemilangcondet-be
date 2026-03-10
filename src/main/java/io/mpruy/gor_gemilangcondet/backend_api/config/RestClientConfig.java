package io.mpruy.gor_gemilangcondet.backend_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Konfigurasi Spring {@link RestClient} untuk memanggil backend Fadhil
 * (reservasi lapangan).
 *
 * <p>Base URL dikonfigurasi via {@code app.fadhil.base-url} di application.yaml.
 */
@Configuration
public class RestClientConfig {

    @Bean("fadhilRestClient")
    public RestClient fadhilRestClient(
            @Value("${app.fadhil.base-url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
