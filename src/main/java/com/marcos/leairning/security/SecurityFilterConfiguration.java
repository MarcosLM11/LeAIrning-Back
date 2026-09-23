package com.marcos.leairning.security;

import com.marcos.leairning.security.auth.AuthProperties;
import com.marcos.leairning.security.oauth2.OAuth2LoginFailureHandler;
import com.marcos.leairning.security.oauth2.OAuth2LoginSuccessHandler;
import com.marcos.leairning.security.oauth2.github.GitHubOAuth2UserService;
import com.marcos.leairning.security.oauth2.google.GoogleOidcUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;
import static org.springframework.http.HttpMethod.GET;

@RequiredArgsConstructor
@EnableWebSecurity
@EnableMethodSecurity
@Configuration(proxyBeanMethods = false)
public class SecurityFilterConfiguration {

    private static final int HIGEST_PRECEDENCE = Ordered.HIGHEST_PRECEDENCE;

    private static final String[] AUTH_PUBLIC_PATTERNS = {
            "/auth/login", "/auth/register", "/auth/verify", "/auth/refresh", "/auth/logout", "/auth/code/exchange"
    };
    private static final String[] JWT_SECURED_PATTERNS = {
            "/auth/logout", "/api/**", "/users/**", "/documents/**", "/conversations/**", "/chat/**", "/quizz/**"
    };
    private static final String[] OAUTH2_PATTERNS = {"/oauth2/**", "/login/oauth2/**"};

    private final AuthProperties authProperties;
    private final GitHubOAuth2UserService gitHubOAuth2UserService;
    private final GoogleOidcUserService googleOidcUserService;
    private final OAuth2LoginSuccessHandler successHandler;
    private final OAuth2LoginFailureHandler failureHandler;

    @Order(HIGEST_PRECEDENCE + 1_000)
    @Bean
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        return chain(http, new String[]{"/actuator/**"}, h -> h.authorizeHttpRequests(request -> request
                .requestMatchers(GET, "/actuator/health").permitAll()
                .anyRequest().denyAll()));
    }

    @Order(HIGEST_PRECEDENCE + 2_000)
    @Bean
    public SecurityFilterChain errorSecurityFilterChain(HttpSecurity http) throws Exception {
        return chain(http, new String[]{"/error"}, h -> h.authorizeHttpRequests(request -> request.anyRequest().permitAll()));
    }

    @Order(HIGEST_PRECEDENCE + 3_000)
    @Bean
    public SecurityFilterChain authSecurityFilterChain(HttpSecurity http) throws Exception {
        return chain(http, AUTH_PUBLIC_PATTERNS, h -> h.authorizeHttpRequests(request -> request.anyRequest().permitAll()));
    }

    @Order(HIGEST_PRECEDENCE + 4_000)
    @Bean
    public SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder, JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        return chain(http, JWT_SECURED_PATTERNS, h -> h
                .authorizeHttpRequests(request -> request.anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                        .decoder(jwtDecoder)
                        .jwtAuthenticationConverter(jwtAuthenticationConverter))));
    }

    @Order(HIGEST_PRECEDENCE + 6_000)
    @Bean
    public SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http) throws Exception {
        return chain(http, OAUTH2_PATTERNS, h -> h
                .authorizeHttpRequests(request -> request.anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(gitHubOAuth2UserService)
                                .oidcUserService(googleOidcUserService))
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)));
    }

    @Order(HIGEST_PRECEDENCE + 50_000)
    @Bean
    public SecurityFilterChain denyAllSecurityFilterChain(HttpSecurity http) throws Exception {
        return chain(http, new String[0], h -> h.authorizeHttpRequests(request -> request.anyRequest().denyAll()));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        var configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(authProperties.getFrontendUrl()));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Conversation-Id", "Accept"));
        configuration.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private SecurityFilterChain chain(HttpSecurity http, String[] patterns, Customizer<HttpSecurity> configurer) throws Exception {
        if (patterns.length > 0) http.securityMatcher(patterns);
        configurer.customize(http);
        http.cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .contentTypeOptions(Customizer.withDefaults())
                        .httpStrictTransportSecurity(h -> h
                                .maxAgeInSeconds(31_536_000)
                                .includeSubDomains(true)));
        return http.build();
    }
}