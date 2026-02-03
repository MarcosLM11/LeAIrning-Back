package com.marcos.leairning.security.token;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.flogger.Flogger;
import lombok.val;
import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.stereotype.Service;
import java.util.Optional;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;

@Flogger
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TokenPairServiceImpl implements TokenPairService {

    Cache<String, TokenPair> cache;

    @Override
    public String add(TokenPair tokenPair) {
        val code = randomUUID().toString();

        log.atFine().log("Creating token code: %s ", code);

        cache.put(code, tokenPair);

        return code;
    }

    @Override
    public Optional<TokenPair> find(String code) {
        log.atFine().log("Fetching token code: %s", code);

        val pair = cache.getIfPresent(code);

        return ofNullable(pair);
    }

    @Override
    public void remove(String code) {
        cache.invalidate(code);
    }

}
