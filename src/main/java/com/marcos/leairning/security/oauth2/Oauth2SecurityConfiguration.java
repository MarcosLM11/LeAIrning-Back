package com.marcos.leairning.security.oauth2;

import com.marcos.leairning.security.AbstractSecurityConfiguration;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class Oauth2SecurityConfiguration extends AbstractSecurityConfiguration {

    private static final String OAUTH2_SECURITY_FILTER_CHAIN = "oauth2SecurityFilterChain";
    private static final String OAUTH2_AUTHORIZATION_PATTERN = "/oauth2/**";
    private static final String OAUTH2_CALLBACK_PATTERN = "/login/oauth2/**";

    Oauth2SuccessHandler handler;

    @SneakyThrows
    @Order(HIGEST_PRECEDENCE + 6_000)
    @Bean(OAUTH2_SECURITY_FILTER_CHAIN)
    public SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http) {
        http.securityMatcher(OAUTH2_AUTHORIZATION_PATTERN, OAUTH2_CALLBACK_PATTERN)
                .authorizeHttpRequests(
                        request -> request.anyRequest().authenticated()
                )
                .oauth2Login(
                        oauth2 -> oauth2.successHandler(handler)
                );
        return buildWithDefaults(http);
    }
}