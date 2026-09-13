package com.propstack.organization.client;

/** Raw shape of a Keycloak user, as returned by the Admin REST API's user-search endpoint. */
public record KeycloakUserSummary(String id, String username, String email) {
}
