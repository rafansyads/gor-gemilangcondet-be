package io.mpruy.gor_gemilangcondet.backend_api.security.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

    private final ClientIpResolver resolver = new ClientIpResolver();

    @Test
    @DisplayName("Should resolve first IPv4 from X-Forwarded-For")
    void resolveFromXForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.15, 10.0.0.1");
        request.setRemoteAddr("127.0.0.1");

        assertEquals("203.0.113.15", resolver.resolveClientIpv4(request));
    }

    @Test
    @DisplayName("Should resolve IPv4 from Forwarded header when X-Forwarded-For is missing")
    void resolveFromForwarded() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Forwarded", "for=198.51.100.7;proto=https");
        request.setRemoteAddr("127.0.0.1");

        assertEquals("198.51.100.7", resolver.resolveClientIpv4(request));
    }

    @Test
    @DisplayName("Should fallback to remote address when proxy headers are not IPv4")
    void resolveFromRemoteAddressFallback() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "unknown");
        request.setRemoteAddr("192.0.2.10");

        assertEquals("192.0.2.10", resolver.resolveClientIpv4(request));
    }
}
