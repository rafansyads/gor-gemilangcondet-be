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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import io.mpruy.gor_gemilangcondet.backend_api.security.ratelimit.IpRateLimitBucketService;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Bucket4j rate limiting.
 * Uses full application context with MockMvc so the Bucket4j servlet filter is
 * active.
 *
 * Rate-limit tiers (from application.yaml):
 * - /api/auth/** → 10 requests/5 minutes
 * - POST|PUT|DELETE (non-auth) → 20 requests/minute
 * - GET (non-auth) → 125 requests/minute
 *
 * Clears the in-memory bucket registry before each test to reset limits.
 */
@SpringBootTest
@ActiveProfiles("h2test")
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RateLimitingIntegrationTest {

    private static final String TEST_CLIENT_IP = "203.0.113.10";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IpRateLimitBucketService rateLimitBucketService;

    @BeforeEach
    void resetRateLimitBuckets() {
        rateLimitBucketService.clearAll();
    }

    @Test
    @Order(1)
    @DisplayName("Auth endpoint rate limit: 11th request within 5 minutes returns 429")
    void authEndpoint_ExceedsLimit_Returns429() throws Exception {
        // The first 10 should NOT be 429 (they may be 200 or other errors, but not
        // rate-limited)
        for (int i = 0; i < 10; i++) {
            MvcResult result = mockMvc.perform(post("/auth/logout")
                    .header("X-Forwarded-For", TEST_CLIENT_IP))
                    .andReturn();
            int statusCode = result.getResponse().getStatus();
            assertNotEquals429(statusCode, i + 1);
        }

        // 11th request should be rate-limited
        mockMvc.perform(post("/auth/logout")
                .header("X-Forwarded-For", TEST_CLIENT_IP))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(
                        jsonPath("$.message").value(startsWith("Terlalu banyak permintaan. Silakan coba lagi dalam ")))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.data.rule").value("auth"))
                .andExpect(jsonPath("$.data.retryAfterSeconds").value(greaterThan(0)));
    }

    @Test
    @Order(2)
    @DisplayName("CUD non-auth endpoint rate limit: 21st POST returns 429")
    void cudEndpoint_ExceedsLimit_Returns429() throws Exception {
        int rateLimitedAt = -1;

        // Use a non-auth path with no handler mapping.
        // Request still passes through rate limiter first, then returns 401/404.
        for (int i = 0; i < 40; i++) {
            MvcResult result = mockMvc.perform(post("/rate-limit/non-existent-cud")
                    .header("X-Forwarded-For", TEST_CLIENT_IP))
                    .andReturn();

            if (result.getResponse().getStatus() == 429) {
                rateLimitedAt = i + 1;
                break;
            }
        }

        assertTrue(rateLimitedAt > 0, "Expected 429 rate limit but never received it after 40 POST requests");
        assertTrue(rateLimitedAt >= 21, "Rate limit triggered too early at request " + rateLimitedAt);

        mockMvc.perform(post("/rate-limit/non-existent-cud")
                .header("X-Forwarded-For", TEST_CLIENT_IP))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(
                        jsonPath("$.message").value(startsWith("Terlalu banyak permintaan. Silakan coba lagi dalam ")))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.data.rule").value("cud"))
                .andExpect(jsonPath("$.data.retryAfterSeconds").value(greaterThan(0)));
    }

    @Test
    @Order(3)
    @DisplayName("GET non-auth endpoint rate limit: requests eventually return 429")
    void getEndpoint_ExceedsLimit_Returns429() throws Exception {
        // Use a non-auth path with no handler mapping.
        // Request still passes through rate limiter first, then returns 401/404.
        int rateLimitedAt = -1;
        for (int i = 0; i < 220; i++) {
            MvcResult result = mockMvc.perform(get("/rate-limit/non-existent-read")
                    .header("X-Forwarded-For", TEST_CLIENT_IP))
                    .andReturn();

            if (result.getResponse().getStatus() == 429) {
                rateLimitedAt = i + 1;
                break;
            }
        }

        assertTrue(rateLimitedAt > 0, "Expected 429 rate limit but never received it after 220 GET requests");
        assertTrue(rateLimitedAt >= 126, "Rate limit triggered too early at request " + rateLimitedAt);

        mockMvc.perform(get("/rate-limit/non-existent-read")
                .header("X-Forwarded-For", TEST_CLIENT_IP))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(
                        jsonPath("$.message").value(startsWith("Terlalu banyak permintaan. Silakan coba lagi dalam ")))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.data.rule").value("read"))
                .andExpect(jsonPath("$.data.retryAfterSeconds").value(greaterThan(0)));
    }

    private void assertNotEquals429(int statusCode, int requestNumber) {
        if (statusCode == 429) {
            throw new AssertionError("Request " + requestNumber + " should not be rate-limited (429)");
        }
    }
}
