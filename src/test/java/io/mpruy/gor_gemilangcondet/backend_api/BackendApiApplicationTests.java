package io.mpruy.gor_gemilangcondet.backend_api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Tes pemuatan konteks aplikasi Spring.
 * Memastikan seluruh bean dan konfigurasi dapat dimuat tanpa error.
 */
@SpringBootTest
@ActiveProfiles("h2test")
class BackendApiApplicationTests {

    @Test
    void contextLoads() {
    }

}
