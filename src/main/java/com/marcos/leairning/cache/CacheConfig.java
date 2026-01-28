package com.marcos.leairning.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.jcache.configuration.CaffeineConfiguration;
import com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.cache.Caching;
import java.util.OptionalLong;
import static lombok.AccessLevel.PRIVATE;

@EnableCaching
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CaffeineCacheProperties.class)
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class CacheConfig {

    CaffeineCacheProperties properties;

    // Para @Cacheable de Spring
    @Bean
    CacheManager cacheManager() {
        var manager = new CaffeineCacheManager();

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

    // Para Bucket4j rate limiting
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