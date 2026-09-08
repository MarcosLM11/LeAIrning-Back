package com.marcos.leairning.security.token;

import com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.Optional;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;

@Service
public class TokenPairServiceImpl implements TokenPairService {

    private static final Logger log = LoggerFactory.getLogger(TokenPairServiceImpl.class);
    private final Cache<String, TokenPair> cache;

    public TokenPairServiceImpl(Cache<String, TokenPair> cache) {
        this.cache = cache;
    }

    @Override
    public String add(TokenPair tokenPair) {
        var code = randomUUID().toString();
        log.info("Creating token code: {} ", code);
        cache.put(code, tokenPair);
        return code;
    }

    @Override
    public Optional<TokenPair> find(String code) {
        log.info("Fetching token code: {}", code);
        var pair = cache.getIfPresent(code);
        return ofNullable(pair);
    }

    @Override
    public void remove(String code) {
        cache.invalidate(code);
    }

}
