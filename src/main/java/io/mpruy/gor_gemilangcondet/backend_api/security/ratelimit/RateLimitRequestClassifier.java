package io.mpruy.gor_gemilangcondet.backend_api.security.ratelimit;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class RateLimitRequestClassifier {

    public RateLimitRule classify(HttpServletRequest request) {
        String normalizedPath = normalizePath(request);
        if (isAuthPath(normalizedPath)) {
            return RateLimitRule.AUTH;
        }

        String method = request.getMethod();
        if (HttpMethod.POST.matches(method)
                || HttpMethod.PUT.matches(method)
                || HttpMethod.PATCH.matches(method)
                || HttpMethod.DELETE.matches(method)) {
            return RateLimitRule.CUD;
        }

        if (HttpMethod.GET.matches(method)) {
            return RateLimitRule.READ;
        }

        return null;
    }

    private String normalizePath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        if (requestUri == null || requestUri.isBlank()) {
            return "/";
        }

        String contextPath = request.getContextPath();
        String normalizedPath = requestUri;
        if (contextPath != null && !contextPath.isBlank() && normalizedPath.startsWith(contextPath)) {
            normalizedPath = normalizedPath.substring(contextPath.length());
        }

        if (normalizedPath.isBlank()) {
            return "/";
        }

        return normalizedPath.startsWith("/") ? normalizedPath : "/" + normalizedPath;
    }

    private boolean isAuthPath(String path) {
        return "/auth".equals(path) || path.startsWith("/auth/");
    }
}
