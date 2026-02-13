package com.marcos.leairning.security.oauth2;

import lombok.extern.flogger.Flogger;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Service to fetch the primary verified email from GitHub API.
 * GitHub's /user endpoint may not return the email if it's not public,
 * so we need to call /user/emails with the access token.
 */
@Flogger
@Service
public class GitHubEmailService {

    private final WebClient webClient;

    public GitHubEmailService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.github.com")
                .build();
    }

    /**
     * Fetches the primary verified email from GitHub.
     * If no primary email is found, returns the first verified email.
     * If permission denied (403), returns null so caller can handle fallback.
     * 
     * @param accessToken GitHub OAuth2 access token
     * @return Primary verified email address, or null if permission denied
     * @throws IllegalStateException if no verified email is found (and permission was granted)
     */
    public String getPrimaryEmail(String accessToken) {
        log.atFine().log("Fetching primary email from GitHub");
        
        try {
            List<Map<String, Object>> emails = webClient.get()
                    .uri("/user/emails")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github.v3+json")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .block();

            if (emails == null || emails.isEmpty()) {
                log.atWarning().log("No emails found for GitHub user");
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
                log.atFine().log("Found primary verified email from GitHub");
                return primaryEmail;
            }

            // Fallback: use first verified email
            String firstVerified = emails.stream()
                    .filter(email -> Boolean.TRUE.equals(email.get("verified")))
                    .map(email -> (String) email.get("email"))
                    .findFirst()
                    .orElse(null);

            if (firstVerified != null) {
                log.atFine().log("Found verified email from GitHub (not primary)");
                return firstVerified;
            }

            log.atWarning().log("No verified email found for GitHub user");
            throw new IllegalStateException("No verified email found for GitHub user. Please verify your email on GitHub.");

        } catch (WebClientResponseException.Forbidden e) {
            // 403 Forbidden - user didn't grant user:email scope
            log.atWarning().log("Permission denied (403) when fetching GitHub emails. " +
                    "The user may need to re-authorize the app with email scope. " +
                    "Falling back to alternative email generation.");
            return null;
        } catch (Exception e) {
            log.atSevere().withCause(e).log("Failed to fetch email from GitHub");
            throw new RuntimeException("Failed to fetch GitHub email: " + e.getMessage(), e);
        }
    }
}
