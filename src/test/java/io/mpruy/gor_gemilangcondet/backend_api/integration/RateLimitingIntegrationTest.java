package io.mpruy.gor_gemilangcondet.backend_api.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.cache.CacheManager;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Bucket4j rate limiting.
 * Uses full application context with MockMvc so the Bucket4j servlet filter is
 * active.
 *
 * Rate-limit tiers (from application.yaml):
 * - /api/auth/** → 5 requests/minute
 * - POST|PUT|DELETE (non-auth) → 20 requests/minute
 * - GET (non-auth) → 100 requests/minute
 *
 * Clears the rate-limit cache before each test to reset buckets.
 */
@SpringBootTest
@ActiveProfiles("h2test")
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RateLimitingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CacheManager jCacheManager;

    @BeforeEach
    void resetRateLimitBuckets() {
        // Clear all entries in the rate-limit cache to reset buckets between tests
        javax.cache.Cache<Object, Object> cache = jCacheManager.getCache("rate-limit-buckets");
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Auth endpoint rate limit: 6th request within 1 minute returns 429")
    void authEndpoint_ExceedsLimit_Returns429() throws Exception {
        String loginBody = """
                {
                  "data": {
                    "username": "rateLimitUser",
                    "password": "somePassword"
                  }
                }
                """;

        // The first 5 should NOT be 429 (they may be 401 or other errors, but not
        // rate-limited)
        for (int i = 0; i < 5; i++) {
            MvcResult result = mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody))
                    .andReturn();
            int statusCode = result.getResponse().getStatus();
            assertNotEquals429(statusCode, i + 1);
        }

        // 6th request should be rate-limited
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @Order(2)
    @DisplayName("CUD non-auth endpoint rate limit: 21st POST returns 429")
    void cudEndpoint_ExceedsLimit_Returns429() throws Exception {
        LocalDate baseDate = LocalDate.of(2026, 3, 10);
        int rateLimitedAt = -1;

        // Use public test-booking endpoint so requests are not blocked by auth before
        // Bucket4j evaluates them.
        for (int i = 0; i < 40; i++) {
            int courtId = (i % 3) + 1;
            String bookingBody = String.format(
                    "{\"courtId\":%d,\"date\":\"%s\",\"time\":\"12:00\",\"customerName\":\"rate-%d\"}",
                    courtId,
                    baseDate.plusDays(i),
                    i);

            MvcResult result = mockMvc.perform(post("/test/book")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(bookingBody))
                    .andReturn();

            if (result.getResponse().getStatus() == 429) {
                rateLimitedAt = i + 1;
                break;
            }
        }

        assertTrue(rateLimitedAt > 0, "Expected 429 rate limit but never received it after 40 POST requests");
        assertTrue(rateLimitedAt >= 21, "Rate limit triggered too early at request " + rateLimitedAt);
    }

    @Test
    @Order(3)
    @DisplayName("GET non-auth endpoint rate limit: requests eventually return 429")
    void getEndpoint_ExceedsLimit_Returns429() throws Exception {
        // Use a static endpoint to produce a fast burst so token refill does not mask
        // the rate limit in tests.
        int rateLimitedAt = -1;
        for (int i = 0; i < 180; i++) {
            MvcResult result = mockMvc.perform(get("/test-schedule.html"))
                    .andReturn();

            if (result.getResponse().getStatus() == 429) {
                rateLimitedAt = i + 1;
                break;
            }
        }

        assertTrue(rateLimitedAt > 0, "Expected 429 rate limit but never received it after 180 GET requests");
        assertTrue(rateLimitedAt >= 100, "Rate limit triggered too early at request " + rateLimitedAt);
    }

    private void assertNotEquals429(int statusCode, int requestNumber) {
        if (statusCode == 429) {
            throw new AssertionError("Request " + requestNumber + " should not be rate-limited (429)");
        }
    }
}
