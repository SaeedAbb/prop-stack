package com.propstack.organization.organization.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.propstack.organization.organization.exception.MissingOrganizationClaimException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class CurrentOrganizationResolverTest {

    private final CurrentOrganizationResolver resolver = new CurrentOrganizationResolver();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requireCurrentOrganizationId_returnsFirstOrganization_whenClaimPresent() {
        authenticateWithOrganizations(List.of("example-org", "second-org"));

        String organizationId = resolver.requireCurrentOrganizationId();

        assertThat(organizationId).isEqualTo("example-org");
    }

    @Test
    void requireCurrentOrganizationId_throws_whenNoAuthenticationPresent() {
        assertThatThrownBy(resolver::requireCurrentOrganizationId)
                .isInstanceOf(MissingOrganizationClaimException.class);
    }

    @Test
    void requireCurrentOrganizationId_throws_whenAuthenticationIsNotJwtBased() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "pw"));

        assertThatThrownBy(resolver::requireCurrentOrganizationId)
                .isInstanceOf(MissingOrganizationClaimException.class);
    }

    @Test
    void requireCurrentOrganizationId_throws_whenOrganizationClaimEmpty() {
        authenticateWithOrganizations(List.of());

        assertThatThrownBy(resolver::requireCurrentOrganizationId)
                .isInstanceOf(MissingOrganizationClaimException.class);
    }

    private void authenticateWithOrganizations(List<String> organizations) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user-1")
                .claim("organization", organizations)
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
