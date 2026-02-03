package com.marcos.leairning.security.token;

import java.util.Optional;

public interface TokenPairService {

    String add(TokenPair tokenPair);

    Optional<TokenPair> find(String code);

    void remove(String code);
}
