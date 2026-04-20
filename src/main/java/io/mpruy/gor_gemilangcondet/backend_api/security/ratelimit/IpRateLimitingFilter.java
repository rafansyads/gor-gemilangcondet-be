package io.mpruy.gor_gemilangcondet.backend_api.security.ratelimit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import tools.jackson.databind.ObjectMapper;

import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@ConditionalOnBean({
        RateLimitRequestClassifier.class,
        ClientIpResolver.class,
        IpRateLimitBucketService.class
})
public class IpRateLimitingFilter extends OncePerRequestFilter {

    private static final String MESSAGE_PREFIX = "Terlalu banyak permintaan. Silakan coba lagi dalam ";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
            .withZone(ZoneId.of("Asia/Jakarta"));

    private final RateLimitRequestClassifier requestClassifier;
    private final ClientIpResolver clientIpResolver;
    private final IpRateLimitBucketService bucketService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IpRateLimitingFilter(
            RateLimitRequestClassifier requestClassifier,
            ClientIpResolver clientIpResolver,
            IpRateLimitBucketService bucketService) {
        this.requestClassifier = requestClassifier;
        this.clientIpResolver = clientIpResolver;
        this.bucketService = bucketService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        RateLimitRule rule = requestClassifier.classify(request);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIpv4Address = clientIpResolver.resolveClientIpv4(request);
        ConsumptionProbe probe = bucketService.tryConsume(rule, clientIpv4Address);

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = toRetryAfterSeconds(probe.getNanosToWaitForRefill());
        String waitDuration = formatWaitDuration(retryAfterSeconds);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        body.put("message", MESSAGE_PREFIX + waitDuration + ".");
        body.put("timestamp", TIMESTAMP_FORMATTER.format(ZonedDateTime.now(ZoneId.of("Asia/Jakarta"))));
        body.put("data",
                new RateLimitErrorData(retryAfterSeconds, rule.name().toLowerCase(Locale.ROOT), clientIpv4Address));

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));

        objectMapper.writeValue(response.getWriter(), body);
    }

    private long toRetryAfterSeconds(long nanosToWait) {
        if (nanosToWait <= 0) {
            return 1;
        }
        long roundedUpSeconds = TimeUnit.NANOSECONDS.toSeconds(nanosToWait + 999_999_999L);
        return Math.max(1, roundedUpSeconds);
    }

    private String formatWaitDuration(long totalSeconds) {
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        if (minutes > 0 && seconds > 0) {
            return minutes + " menit " + seconds + " detik";
        }
        if (minutes > 0) {
            return minutes + " menit";
        }
        return seconds + " detik";
    }

    private record RateLimitErrorData(long retryAfterSeconds, String rule, String ipAddress) {
    }
}
