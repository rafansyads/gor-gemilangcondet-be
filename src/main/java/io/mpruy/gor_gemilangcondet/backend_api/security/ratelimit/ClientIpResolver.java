package io.mpruy.gor_gemilangcondet.backend_api.security.ratelimit;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class ClientIpResolver {

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$");

    public String resolveClientIpv4(HttpServletRequest request) {
        String fromXForwardedFor = extractFromXForwardedFor(request.getHeader("X-Forwarded-For"));
        if (fromXForwardedFor != null) {
            return fromXForwardedFor;
        }

        String fromForwarded = extractFromForwarded(request.getHeader("Forwarded"));
        if (fromForwarded != null) {
            return fromForwarded;
        }

        String fromRemoteAddress = normalizeCandidate(request.getRemoteAddr());
        if (isValidIpv4(fromRemoteAddress)) {
            return fromRemoteAddress;
        }

        return "127.0.0.1";
    }

    private String extractFromXForwardedFor(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }

        String[] parts = headerValue.split(",");
        for (String part : parts) {
            String candidate = normalizeCandidate(part);
            if (isValidIpv4(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private String extractFromForwarded(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }

        String[] entries = headerValue.split(",");
        for (String entry : entries) {
            String[] parameters = entry.split(";");
            for (String parameter : parameters) {
                String trimmedParameter = parameter.trim();
                if (!trimmedParameter.regionMatches(true, 0, "for=", 0, 4)) {
                    continue;
                }
                String candidate = normalizeCandidate(trimmedParameter.substring(4));
                if (isValidIpv4(candidate)) {
                    return candidate;
                }
            }
        }

        return null;
    }

    private String normalizeCandidate(String rawCandidate) {
        if (rawCandidate == null) {
            return null;
        }

        String candidate = rawCandidate.trim();
        if (candidate.isBlank()) {
            return null;
        }

        if (candidate.startsWith("\"") && candidate.endsWith("\"") && candidate.length() >= 2) {
            candidate = candidate.substring(1, candidate.length() - 1);
        }

        if (candidate.startsWith("[")) {
            int closingBracketIndex = candidate.indexOf(']');
            if (closingBracketIndex > 0) {
                candidate = candidate.substring(1, closingBracketIndex);
            }
        }

        if (candidate.startsWith("::ffff:")) {
            candidate = candidate.substring("::ffff:".length());
        }

        if ("::1".equals(candidate)) {
            return "127.0.0.1";
        }

        int colonCount = countOccurrences(candidate, ':');
        if (colonCount == 1 && candidate.contains(".")) {
            int separatorIndex = candidate.indexOf(':');
            if (separatorIndex > 0) {
                candidate = candidate.substring(0, separatorIndex);
            }
        }

        return candidate;
    }

    private boolean isValidIpv4(String value) {
        return value != null && IPV4_PATTERN.matcher(value).matches();
    }

    private int countOccurrences(String value, char target) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == target) {
                count++;
            }
        }
        return count;
    }
}
