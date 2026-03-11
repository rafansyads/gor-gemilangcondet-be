package io.mpruy.gor_gemilangcondet.backend_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point aplikasi GOR Gemilang Condet Backend.
 *
 * <p>{@code @EnableAsync} diperlukan agar broadcast WebSocket pada
 * {@link io.mpruy.gor_gemilangcondet.backend_api.event.ScheduleBroadcastListener}
 * berjalan di thread terpisah tanpa memblokir transaksi utama.
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
public class BackendApiApplication {

	public static void main(String[] args) {
			SpringApplication.run(BackendApiApplication.class, args);
	}

}
