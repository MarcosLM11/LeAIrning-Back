package com.marcos.leairning.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.jcache.configuration.CaffeineConfiguration;
import com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider;
import com.marcos.leairning.security.auth.AuthProperties;
import com.marcos.leairning.security.auth.LoginLockoutProperties;
import com.marcos.leairning.security.token.TokenPair;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.cache.Caching;
import java.time.Duration;
import java.util.OptionalLong;

@EnableCaching
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({CaffeineCacheProperties.class, AuthProperties.class, LoginLockoutProperties.class})
@RequiredArgsConstructor
public class CacheConfig {

    private final CaffeineCacheProperties properties;
    private final LoginLockoutProperties lockoutProperties;

    // Para @Cacheable de Spring
    @Bean
    CacheManager cacheManager() {
        var manager = new CaffeineCacheManager();
        manager.registerCustomCache("documents", caffeineAccess(properties.getDocumentsMaximumSize(), properties.getDocumentsExpireAfterAccess()));
        manager.registerCustomCache("users", caffeineAccess(properties.getUsersMaximumSize(), properties.getUsersExpireAfterAccess()));
        return manager;
    }

    @Bean
    Cache<String, TokenPair> tokenPairCache() {
        return caffeineWrite(10_000, Duration.ofMinutes(5));
    }

    @Bean
    Cache<String, String> verificationTokenCache() {
        return caffeineWrite(10_000, Duration.ofHours(24));
    }

    @Bean
    Cache<String, Integer> loginAttemptsCache() {
        return caffeineWrite(lockoutProperties.getCacheMaxSize(), lockoutProperties.getLockoutDuration());
    }

    private com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeineAccess(long maxSize, Duration expireAfterAccess) {
        return Caffeine.newBuilder().maximumSize(maxSize).expireAfterAccess(expireAfterAccess).build();
    }

    private <K, V> Cache<K, V> caffeineWrite(long maxSize, Duration expireAfterWrite) {
        return Caffeine.newBuilder().maximumSize(maxSize).expireAfterWrite(expireAfterWrite).build();
    }

    //Bucket4j rate limiting config
    @Bean
    javax.cache.CacheManager jCacheCacheManager() {
        var provider = Caching.getCachingProvider(CaffeineCachingProvider.class.getName());
        var cacheManager = provider.getCacheManager();
        var config = new CaffeineConfiguration<>();
        config.setMaximumSize(OptionalLong.of(properties.getRateLimitMaximumSize()));
        config.setExpireAfterAccess(OptionalLong.of(properties.getRateLimitExpireAfterAccess().toNanos()));
        cacheManager.createCache("rate-limit-buckets", config);
        return cacheManager;
    }
}