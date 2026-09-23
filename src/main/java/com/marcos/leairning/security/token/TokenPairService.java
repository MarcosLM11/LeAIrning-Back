package com.marcos.leairning.security.token;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Optional;
import static java.util.UUID.randomUUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenPairService {

    private final Cache<String, TokenPair> cache;

    public String add(TokenPair tokenPair) {
        var code = randomUUID().toString();
        log.info("Creating token code: {}", code);
        cache.put(code, tokenPair);
        return code;
    }

    public Optional<TokenPair> find(String code) {
        return Optional.ofNullable(cache.getIfPresent(code));
    }

    public void remove(String code) {
        cache.invalidate(code);
    }
}