package io.mpruy.gor_gemilangcondet.backend_api.security.ratelimit;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;

@Component
public class IpRateLimitBucketService {

    private final Cache<String, Bucket> buckets;

    private final Bandwidth authBandwidth;
    private final Bandwidth cudBandwidth;
    private final Bandwidth readBandwidth;

    public IpRateLimitBucketService(
            @Value("${app.rate-limit.auth.capacity:10}") long authCapacity,
            @Value("${app.rate-limit.auth.window-seconds:300}") long authWindowSeconds,
            @Value("${app.rate-limit.cud.capacity:20}") long cudCapacity,
            @Value("${app.rate-limit.cud.window-seconds:60}") long cudWindowSeconds,
            @Value("${app.rate-limit.read.capacity:125}") long readCapacity,
            @Value("${app.rate-limit.read.window-seconds:60}") long readWindowSeconds) {

        this.authBandwidth = buildBandwidth(authCapacity, authWindowSeconds, "auth");
        this.cudBandwidth = buildBandwidth(cudCapacity, cudWindowSeconds, "cud");
        this.readBandwidth = buildBandwidth(readCapacity, readWindowSeconds, "read");

        this.buckets = Caffeine.newBuilder()
                .expireAfterAccess(1, TimeUnit.HOURS)
                .maximumSize(100_000)
                .build();
    }

    public ConsumptionProbe tryConsume(RateLimitRule rule, String ipv4Address) {
        String bucketKey = rule.name() + ":" + ipv4Address;
        Bucket bucket = buckets.get(bucketKey, key -> newBucket(rule));
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    public void clearAll() {
        buckets.invalidateAll();
    }

    private Bandwidth buildBandwidth(long capacity, long windowSeconds, String ruleName) {
        if (capacity <= 0 || windowSeconds <= 0) {
            throw new IllegalStateException("Rate limit config tidak valid untuk rule " + ruleName);
        }

        Refill refill = Refill.intervally(capacity, Duration.ofSeconds(windowSeconds));
        return Bandwidth.classic(capacity, refill);
    }

    private Bucket newBucket(RateLimitRule rule) {
        Bandwidth bandwidth;
        switch (rule) {
            case AUTH:
                bandwidth = authBandwidth;
                break;
            case CUD:
                bandwidth = cudBandwidth;
                break;
            case READ:
                bandwidth = readBandwidth;
                break;
            default:
                throw new IllegalStateException("Rate limit rule tidak dikenali: " + rule);
        }

        return Bucket.builder()
                .addLimit(bandwidth)
                .build();
    }
}
