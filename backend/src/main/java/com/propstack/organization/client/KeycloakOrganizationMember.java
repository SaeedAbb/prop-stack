package com.propstack.organization.client;

/** Raw shape of a Keycloak organization member, as returned by the Admin REST API. */
public record KeycloakOrganizationMember(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        String membershipType) {
}
