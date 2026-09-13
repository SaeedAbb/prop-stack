package com.propstack.organization.organization.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.propstack.organization.keycloak.KeycloakAdminClient;
import com.propstack.organization.keycloak.KeycloakOrganizationMember;
import com.propstack.organization.keycloak.KeycloakUserSummary;
import com.propstack.organization.organization.dto.AddOrganizationMemberRequest;
import com.propstack.organization.organization.dto.OrganizationMemberResponse;
import com.propstack.organization.organization.exception.NewMemberNameRequiredException;
import com.propstack.organization.organization.exception.SelfRemovalNotAllowedException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class OrganizationAdminServiceTest {

    private static final String ORG_ALIAS = "example-org";
    private static final String ORG_KEYCLOAK_ID = "kc-org-id";

    @Mock
    private CurrentOrganizationResolver currentOrganizationResolver;

    @Mock
    private KeycloakAdminClient keycloakAdminClient;

    @Mock
    private OrganizationMemberMapper organizationMemberMapper;

    private OrganizationAdminService organizationAdminService;

    @BeforeEach
    void setUp() {
        organizationAdminService =
                new OrganizationAdminService(currentOrganizationResolver, keycloakAdminClient, organizationMemberMapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listMembers_returnsMappedMembers() {
        when(currentOrganizationResolver.requireCurrentOrganizationId()).thenReturn(ORG_ALIAS);
        when(keycloakAdminClient.findOrganizationIdByAlias(ORG_ALIAS)).thenReturn(ORG_KEYCLOAK_ID);
        KeycloakOrganizationMember member =
                new KeycloakOrganizationMember("u1", "jdoe", "jdoe@example.com", "Jane", "Doe", true, "MANAGED");
        OrganizationMemberResponse response = OrganizationMemberResponse.builder().id("u1").email("jdoe@example.com").build();
        when(keycloakAdminClient.listMembers(ORG_KEYCLOAK_ID)).thenReturn(List.of(member));
        when(organizationMemberMapper.toResponse(member)).thenReturn(response);

        List<OrganizationMemberResponse> result = organizationAdminService.listMembers();

        assertThat(result).containsExactly(response);
    }

    @Test
    void addMember_addsExistingUser_whenEmailMatchesExistingUser() {
        when(currentOrganizationResolver.requireCurrentOrganizationId()).thenReturn(ORG_ALIAS);
        when(keycloakAdminClient.findOrganizationIdByAlias(ORG_ALIAS)).thenReturn(ORG_KEYCLOAK_ID);
        AddOrganizationMemberRequest request = AddOrganizationMemberRequest.builder().email("existing@example.com").build();
        KeycloakUserSummary existingUser = new KeycloakUserSummary("u2", "existing", "existing@example.com");
        when(keycloakAdminClient.findUserByEmail("existing@example.com")).thenReturn(existingUser);

        organizationAdminService.addMember(request);

        verify(keycloakAdminClient).addExistingMember(ORG_KEYCLOAK_ID, "u2");
        verify(keycloakAdminClient, never()).inviteNewMember(any(), any(), any(), any());
    }

    @Test
    void addMember_invitesNewUser_whenNoExistingUserFound_andNamesProvided() {
        when(currentOrganizationResolver.requireCurrentOrganizationId()).thenReturn(ORG_ALIAS);
        when(keycloakAdminClient.findOrganizationIdByAlias(ORG_ALIAS)).thenReturn(ORG_KEYCLOAK_ID);
        AddOrganizationMemberRequest request = AddOrganizationMemberRequest.builder()
                .email("new@example.com")
                .firstName("New")
                .lastName("Person")
                .build();
        when(keycloakAdminClient.findUserByEmail("new@example.com")).thenReturn(null);

        organizationAdminService.addMember(request);

        verify(keycloakAdminClient).inviteNewMember(ORG_KEYCLOAK_ID, "new@example.com", "New", "Person");
    }

    @Test
    void addMember_throwsNewMemberNameRequired_whenNamesMissingForNewUser() {
        when(currentOrganizationResolver.requireCurrentOrganizationId()).thenReturn(ORG_ALIAS);
        when(keycloakAdminClient.findOrganizationIdByAlias(ORG_ALIAS)).thenReturn(ORG_KEYCLOAK_ID);
        AddOrganizationMemberRequest request = AddOrganizationMemberRequest.builder().email("new@example.com").build();
        when(keycloakAdminClient.findUserByEmail("new@example.com")).thenReturn(null);

        assertThatThrownBy(() -> organizationAdminService.addMember(request))
                .isInstanceOf(NewMemberNameRequiredException.class);

        verify(keycloakAdminClient, never()).inviteNewMember(any(), any(), any(), any());
    }

    @Test
    void removeMember_throwsSelfRemovalNotAllowed_whenRemovingSelf() {
        authenticateAs("current-user-id");

        assertThatThrownBy(() -> organizationAdminService.removeMember("current-user-id"))
                .isInstanceOf(SelfRemovalNotAllowedException.class);

        verify(keycloakAdminClient, never()).removeMember(any(), any());
    }

    @Test
    void removeMember_removesAndLogsOutUser_whenNotSelf() {
        authenticateAs("current-user-id");
        when(currentOrganizationResolver.requireCurrentOrganizationId()).thenReturn(ORG_ALIAS);
        when(keycloakAdminClient.findOrganizationIdByAlias(ORG_ALIAS)).thenReturn(ORG_KEYCLOAK_ID);

        organizationAdminService.removeMember("other-user-id");

        verify(keycloakAdminClient).removeMember(ORG_KEYCLOAK_ID, "other-user-id");
        verify(keycloakAdminClient).logoutUser("other-user-id");
    }

    private void authenticateAs(String subject) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", subject)
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
