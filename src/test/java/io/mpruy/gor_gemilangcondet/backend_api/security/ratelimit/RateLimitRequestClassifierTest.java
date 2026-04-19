package io.mpruy.gor_gemilangcondet.backend_api.security.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class RateLimitRequestClassifierTest {

    private final RateLimitRequestClassifier classifier = new RateLimitRequestClassifier();

    @Test
    @DisplayName("Should classify auth path as AUTH regardless of HTTP method")
    void classifyAuthPathFirst() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/login");
        request.setContextPath("/api");

        assertEquals(RateLimitRule.AUTH, classifier.classify(request));
    }

    @Test
    @DisplayName("Should classify PATCH non-auth request as CUD")
    void classifyPatchAsCud() {
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/courts/1");
        request.setContextPath("/api");

        assertEquals(RateLimitRule.CUD, classifier.classify(request));
    }

    @Test
    @DisplayName("Should classify GET non-auth request as READ")
    void classifyGetAsRead() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        request.setContextPath("/api");

        assertEquals(RateLimitRule.READ, classifier.classify(request));
    }

    @Test
    @DisplayName("Should skip unsupported methods")
    void skipUnsupportedMethod() {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/products");
        request.setContextPath("/api");

        assertNull(classifier.classify(request));
    }
}
