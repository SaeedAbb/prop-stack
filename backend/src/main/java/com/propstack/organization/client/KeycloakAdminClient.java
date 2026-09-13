package com.propstack.organization.client;

import com.propstack.organization.exception.KeycloakAdminApiException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Thin wrapper around Keycloak's Admin REST API for the organization-membership operations this app needs. */
@Slf4j
@Component
public class KeycloakAdminClient {

    private final RestClient restClient;

    public KeycloakAdminClient(KeycloakAdminProperties properties, KeycloakAdminTokenProvider tokenProvider) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.serverUrl() + "/admin/realms/" + properties.realm())
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().setBearerAuth(tokenProvider.getAccessToken());
                    return execution.execute(request, body);
                })
                .build();
    }

    public String findOrganizationIdByAlias(String alias) {
        try {
            KeycloakOrganizationSummary[] organizations = restClient.get()
                    .uri("/organizations?alias={alias}", alias)
                    .retrieve()
                    .body(KeycloakOrganizationSummary[].class);
            if (organizations == null || organizations.length == 0) {
                throw new KeycloakAdminApiException("No Keycloak organization found for alias '" + alias + "'");
            }
            return organizations[0].id();
        } catch (RestClientException e) {
            log.error("Failed to look up Keycloak organization by alias '{}'", alias, e);
            throw new KeycloakAdminApiException("Could not look up organization '" + alias + "'", e);
        }
    }

    public List<KeycloakOrganizationMember> listMembers(String organizationId) {
        try {
            KeycloakOrganizationMember[] members = restClient.get()
                    .uri("/organizations/{orgId}/members", organizationId)
                    .retrieve()
                    .body(KeycloakOrganizationMember[].class);
            return members == null ? List.of() : List.of(members);
        } catch (RestClientException e) {
            log.error("Failed to list members of organization '{}'", organizationId, e);
            throw new KeycloakAdminApiException("Could not list organization members", e);
        }
    }

    public KeycloakUserSummary findUserByEmail(String email) {
        try {
            KeycloakUserSummary[] users = restClient.get()
                    .uri("/users?email={email}&exact=true", email)
                    .retrieve()
                    .body(KeycloakUserSummary[].class);
            return (users == null || users.length == 0) ? null : users[0];
        } catch (RestClientException e) {
            log.error("Failed to look up user by email", e);
            throw new KeycloakAdminApiException("Could not look up user by email", e);
        }
    }

    public void addExistingMember(String organizationId, String userId) {
        try {
            restClient.post()
                    .uri("/organizations/{orgId}/members", organizationId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(userId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Failed to add user '{}' to organization '{}'", userId, organizationId, e);
            throw new KeycloakAdminApiException("Could not add member to organization", e);
        }
    }

    public void inviteNewMember(String organizationId, String email, String firstName, String lastName) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("email", email);
        form.add("firstName", firstName);
        form.add("lastName", lastName);
        try {
            restClient.post()
                    .uri("/organizations/{orgId}/members/invite-user", organizationId)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Failed to invite '{}' to organization '{}'", email, organizationId, e);
            throw new KeycloakAdminApiException("Could not invite new member", e);
        }
    }

    public void removeMember(String organizationId, String userId) {
        try {
            restClient.delete()
                    .uri("/organizations/{orgId}/members/{userId}", organizationId, userId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Failed to remove user '{}' from organization '{}'", userId, organizationId, e);
            throw new KeycloakAdminApiException("Could not remove organization member", e);
        }
    }

    public void logoutUser(String userId) {
        try {
            restClient.post()
                    .uri("/users/{userId}/logout", userId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Failed to force-logout user '{}'", userId, e);
            throw new KeycloakAdminApiException("Could not log out the removed member", e);
        }
    }
}
