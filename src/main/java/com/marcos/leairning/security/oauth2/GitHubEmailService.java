package com.marcos.leairning.security.oauth2;

import io.netty.channel.ChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Service to fetch the primary verified email from GitHub API.
 * GitHub's /user endpoint may not return the email if it's not public,
 * so we need to call /user/emails with the access token.
 */
@Service
public class GitHubEmailService {
    private static final Logger log = LoggerFactory.getLogger(GitHubEmailService.class);

    private final WebClient webClient;

    public GitHubEmailService(WebClient.Builder webClientBuilder) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(10));

        this.webClient = webClientBuilder
                .baseUrl("https://api.github.com")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    public String getPrimaryEmail(String accessToken) {
        log.info("Fetching primary email from GitHub");
        
        try {
            List<Map<String, Object>> emails = webClient.get()
                    .uri("/user/emails")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github.v3+json")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .block();

            if (emails == null || emails.isEmpty()) {
                log.warn("No emails found for GitHub user");
                throw new IllegalStateException("No emails found for GitHub user");
            }

            // Look for primary and verified email
            String primaryEmail = emails.stream()
                    .filter(email -> Boolean.TRUE.equals(email.get("primary")))
                    .filter(email -> Boolean.TRUE.equals(email.get("verified")))
                    .map(email -> (String) email.get("email"))
                    .findFirst()
                    .orElse(null);

            if (primaryEmail != null) {
                log.info("Found primary verified email from GitHub");
                return primaryEmail;
            }

            // Fallback: use first verified email
            String firstVerified = emails.stream()
                    .filter(email -> Boolean.TRUE.equals(email.get("verified")))
                    .map(email -> (String) email.get("email"))
                    .findFirst()
                    .orElse(null);

            if (firstVerified != null) {
                log.info("Found verified email from GitHub (not primary)");
                return firstVerified;
            }

            log.warn("No verified email found for GitHub user");
            throw new IllegalStateException("No verified email found for GitHub user. Please verify your email on GitHub.");

        } catch (WebClientResponseException.Forbidden e) {
            // 403 Forbidden - user didn't grant user:email scope
            log.warn("Permission denied (403) when fetching GitHub emails. " +
                    "The user may need to re-authorize the app with email scope. " +
                    "Falling back to alternative email generation.");
            return null;
        } catch (Exception e) {
            log.warn("Failed to fetch email from GitHub");
            throw new RuntimeException("Failed to fetch GitHub email: " + e.getMessage(), e);
        }
    }
}
