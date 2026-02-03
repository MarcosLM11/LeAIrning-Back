package com.marcos.leairning.security;

import lombok.SneakyThrows;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import static org.springframework.http.HttpMethod.GET;

@EnableWebSecurity
@EnableMethodSecurity
@Configuration(proxyBeanMethods = false)
public class DefaultWebSecurityConfiguration extends AbstractSecurityConfiguration {

    private static final String ACTUATOR_PATTERN = "/actuator/**";
    private static final String ERROR_PATTERN = "/error";
    private static final String ACTUATOR_SECURITY_FILTER_CHAIN = "actuatorSecurityFilterChain";
    private static final String ERROR_SECURITY_FILTER_CHAIN = "errorSecurityFilterChain";
    private static final String AUTH_SECURITY_FILTER_CHAIN = "authSecurityFilterChain";
    private static final String DENY_ALL_SECURITY_FILTER_CHAIN = "denyAllSecurityFilterChain";

    private static final String[] AUTH_PUBLIC_PATTERNS = {
            "/auth/login",
            "/auth/register",
            "/auth/verify",
            "/auth/code/exchange"
    };

    @SneakyThrows
    @Order(HIGEST_PRECEDENCE + 1_000)
    @Bean(ACTUATOR_SECURITY_FILTER_CHAIN)
    public SecurityFilterChain actuatorSecurityConfig(HttpSecurity http) {
        http.securityMatcher(ACTUATOR_PATTERN)
                .authorizeHttpRequests(
                        request -> request.requestMatchers(GET).permitAll()
                );
        return buildWithDefaults(http);
    }

    @SneakyThrows
    @Order(HIGEST_PRECEDENCE + 2_000)
    @Bean(ERROR_SECURITY_FILTER_CHAIN)
    public SecurityFilterChain errorSecurityFilterChain(HttpSecurity http) {
        http.securityMatcher(ERROR_PATTERN)
                .authorizeHttpRequests(
                        request -> request.anyRequest().permitAll()
                );
        return buildWithDefaults(http);
    }

    @SneakyThrows
    @Order(HIGEST_PRECEDENCE + 3_000)
    @Bean(AUTH_SECURITY_FILTER_CHAIN)
    public SecurityFilterChain authSecurityFilterChain(HttpSecurity http) {
        http.securityMatcher(AUTH_PUBLIC_PATTERNS)
                .authorizeHttpRequests(
                        request -> request.anyRequest().permitAll()
                );
        return buildWithDefaults(http);
    }

    @SneakyThrows
    @Order(HIGEST_PRECEDENCE + 50_000)
    @Bean(DENY_ALL_SECURITY_FILTER_CHAIN)
    public SecurityFilterChain denyAllSecurityFilterChain(HttpSecurity http) {
        http.authorizeHttpRequests(
                request -> request.anyRequest().denyAll()
        );
        return buildWithDefaults(http);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}