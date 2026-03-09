package io.mpruy.gor_gemilangcondet.backend_api.config;

import com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Exposes a Caffeine-backed JCache CacheManager as a Spring bean.
     * Spring Boot's JCacheCacheConfiguration wraps it into a Spring CacheManager.
     * Bucket4j (cache-to-use: jcache) resolves this bean directly to find
     * the "rate-limit-buckets" cache.
     */
    @Bean
    public CacheManager jCacheManager() {
        CacheManager cacheManager = Caching
                .getCachingProvider(CaffeineCachingProvider.class.getName())
                .getCacheManager();

        cacheManager.createCache("rate-limit-buckets", new MutableConfiguration<>()
                .setStoreByValue(false)
                .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(
                        new Duration(TimeUnit.HOURS, 1)))); // Hapus IP dari cache jika nganggur 1 jam

        return cacheManager;
    }
}