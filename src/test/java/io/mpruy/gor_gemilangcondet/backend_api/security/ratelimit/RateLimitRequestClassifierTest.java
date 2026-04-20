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
    @DisplayName("Should classify POST PUT and DELETE as CUD")
    void classifyOtherCudMethods() {
        MockHttpServletRequest post = new MockHttpServletRequest("POST", "/orders");
        MockHttpServletRequest put = new MockHttpServletRequest("PUT", "/orders/1");
        MockHttpServletRequest delete = new MockHttpServletRequest("DELETE", "/orders/1");

        assertEquals(RateLimitRule.CUD, classifier.classify(post));
        assertEquals(RateLimitRule.CUD, classifier.classify(put));
        assertEquals(RateLimitRule.CUD, classifier.classify(delete));
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

    @Test
    @DisplayName("Should classify /auth exact path as AUTH")
    void classifyExactAuthPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth");

        assertEquals(RateLimitRule.AUTH, classifier.classify(request));
    }

    @Test
    @DisplayName("Should normalize blank uri to root path")
    void normalizeBlankUri() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("   ");

        assertEquals(RateLimitRule.READ, classifier.classify(request));
    }

    @Test
    @DisplayName("Should normalize uri equal to context path")
    void normalizeContextOnlyPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api");
        request.setContextPath("/api");

        assertEquals(RateLimitRule.READ, classifier.classify(request));
    }
}
