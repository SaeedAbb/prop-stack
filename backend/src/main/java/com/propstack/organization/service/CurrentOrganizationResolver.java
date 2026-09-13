package com.propstack.organization;

import java.util.List;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Resolves the calling user's organization from their JWT's {@code organization} claim -
 * a JSON array of the Keycloak organization alias(es) the user belongs to
 * (e.g. {@code ["example-org"]}), populated by Keycloak's built-in "organization" client scope.
 *
 * <p>v1 assumes exactly one organization per user; if a token ever carries more than one,
 * the first is used.
 */
@Component
public class CurrentOrganizationResolver {

    public String requireCurrentOrganizationId() {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwtAuth)) {
            throw new MissingOrganizationClaimException();
        }

        Jwt jwt = jwtAuth.getToken();
        List<String> organizations = jwt.getClaimAsStringList("organization");
        if (organizations == null || organizations.isEmpty()) {
            throw new MissingOrganizationClaimException();
        }

        return organizations.get(0);
    }
}
