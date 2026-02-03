package com.marcos.leairning.security.code;

import com.marcos.leairning.security.token.TokenPair;
import com.marcos.leairning.security.token.TokenPairService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class AuthCodeExchangeController {

    TokenPairService tokenPairService;

    @GetMapping("/code/exchange")
    public TokenPair exchange(@RequestParam String code) {

        val pair = tokenPairService.find(code);

        if (pair.isEmpty()) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(401),"Invalid or Expired authentication code");
        }

        tokenPairService.remove(code);

        return pair.get();
    }
}
