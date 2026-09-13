package com.propstack.organization.client;

/** Raw shape of a Keycloak organization, as returned by the Admin REST API. */
public record KeycloakOrganizationSummary(String id, String name, String alias, boolean enabled) {
}
