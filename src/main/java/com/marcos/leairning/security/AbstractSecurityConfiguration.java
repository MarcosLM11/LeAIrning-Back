package com.marcos.leairning.security;

import org.springframework.core.Ordered;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

public abstract class AbstractSecurityConfiguration {

    protected static final int HIGEST_PRECEDENCE = Ordered.HIGHEST_PRECEDENCE;

    protected SecurityFilterChain buildWithDefaults(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());
        return http.build();
    }
}