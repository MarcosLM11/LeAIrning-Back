package com.marcos.leairning.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.jcache.configuration.CaffeineConfiguration;
import com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider;
import com.marcos.leairning.security.auth.AuthProperties;
import com.marcos.leairning.security.token.TokenPair;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.cache.Caching;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;
import static lombok.AccessLevel.PRIVATE;

@EnableCaching
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({CaffeineCacheProperties.class, AuthProperties.class})
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class CacheConfig {

    CaffeineCacheProperties properties;

    // Para @Cacheable de Spring
    @Bean
    CacheManager cacheManager() {
        val manager = new CaffeineCacheManager();

        manager.registerCustomCache("documents",
                Caffeine.newBuilder()
                        .maximumSize(properties.getDocumentsMaximumSize())
                        .expireAfterAccess(properties.getDocumentsExpireAfterAccess())
                        .build());

        manager.registerCustomCache("users",
                Caffeine.newBuilder()
                        .maximumSize(properties.getDocumentsMaximumSize())
                        .expireAfterAccess(properties.getDocumentsExpireAfterAccess())
                        .build());

        return manager;
    }

    @Bean
    Cache<String, TokenPair> tokenPairCache() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .build();
    }

    @Bean
    Cache<String, String> verificationTokenCache() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(24, TimeUnit.HOURS)
                .build();
    }

    // Para Bucket4j rate limiting
    @Bean
    javax.cache.CacheManager jCacheCacheManager() {
        val provider = Caching.getCachingProvider(CaffeineCachingProvider.class.getName());
        val cacheManager = provider.getCacheManager();

        val config = new CaffeineConfiguration<>();
        config.setMaximumSize(OptionalLong.of(properties.getRateLimitMaximumSize()));
        config.setExpireAfterAccess(OptionalLong.of(properties.getRateLimitExpireAfterAccess().toNanos()));

        cacheManager.createCache("rate-limit-buckets", config);

        return cacheManager;
    }
}