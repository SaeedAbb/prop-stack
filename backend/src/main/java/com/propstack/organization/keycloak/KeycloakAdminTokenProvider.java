package com.propstack.organization.keycloak;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.propstack.organization.keycloak.exception.KeycloakAdminApiException;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Obtains and caches a client_credentials access token for the {@code propstack-backend}
 * service account, used to call Keycloak's Admin REST API on behalf of the app itself.
 */
@Slf4j
@Component
@EnableConfigurationProperties(KeycloakAdminProperties.class)
public class KeycloakAdminTokenProvider {

    /** Refresh this many seconds before actual expiry, to avoid using a token that expires mid-request. */
    private static final long EXPIRY_SAFETY_MARGIN_SECONDS = 10;

    private final KeycloakAdminProperties properties;
    private final RestClient tokenRestClient;
    private final AtomicReference<CachedToken> cachedToken = new AtomicReference<>();

    public KeycloakAdminTokenProvider(KeycloakAdminProperties properties) {
        this.properties = properties;
        this.tokenRestClient = RestClient.create(properties.serverUrl());
    }

    public String getAccessToken() {
        CachedToken current = cachedToken.get();
        if (current != null && current.isValid()) {
            return current.accessToken();
        }
        CachedToken fresh = fetchToken();
        cachedToken.set(fresh);
        return fresh.accessToken();
    }

    private CachedToken fetchToken() {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", properties.clientId());
        body.add("client_secret", properties.clientSecret());

        try {
            TokenResponse response = tokenRestClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", properties.realm())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(TokenResponse.class);
            if (response == null) {
                throw new KeycloakAdminApiException("Keycloak returned an empty token response");
            }
            return new CachedToken(response.accessToken(), Instant.now().plusSeconds(response.expiresIn()));
        } catch (RestClientException e) {
            log.error("Failed to obtain a Keycloak admin service-account token", e);
            throw new KeycloakAdminApiException("Could not authenticate to Keycloak", e);
        }
    }

    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") Long expiresIn) {
    }

    private record CachedToken(String accessToken, Instant expiresAt) {
        boolean isValid() {
            return Instant.now().isBefore(expiresAt.minusSeconds(EXPIRY_SAFETY_MARGIN_SECONDS));
        }
    }
}
