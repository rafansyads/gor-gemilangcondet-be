package io.mpruy.gor_gemilangcondet.backend_api.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.DateTimeFeature;

/**
 * Konfigurasi Jackson untuk serialisasi tipe waktu Java (Instant, LocalDate,
 * dll.).
 *
 * <p>
 * Jackson 3.x (jackson-databind 3.0.x) menyertakan dukungan Java Time secara
 * bawaan
 * melalui {@code tools.jackson.databind.ext.javatime.JavaTimeInitializer}.
 * Konfigurasi ini menonaktifkan serialisasi tanggal sebagai timestamp numerik
 * sehingga {@code Instant}, {@code LocalDate}, dan {@code LocalDateTime}
 * di-serialize sebagai string ISO-8601.
 */
@Configuration
public class JacksonConfig {

    /**
     * Menonaktifkan serialisasi tanggal sebagai timestamp numerik (Jackson 3.x
     * menggunakan {@link DateTimeFeature} menggantikan
     * {@code SerializationFeature}).
     */
    @Bean
    public JsonMapperBuilderCustomizer jacksonDateTimeCustomizer() {
        return builder -> builder.disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
