package com.marcos.leairning.security.token;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthCodeExchangeController {

    private final TokenPairService tokenPairService;

    @GetMapping("/code/exchange")
    public TokenPair exchange(@RequestParam String code) {
        var pair = tokenPairService.find(code);
        if (pair.isEmpty()) throw new ResponseStatusException(HttpStatusCode.valueOf(401), "Invalid or Expired authentication code");
        tokenPairService.remove(code);
        return pair.get();
    }
}