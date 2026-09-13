package com.propstack.organization;

import com.propstack.organization.dto.AddOrganizationMemberRequest;
import com.propstack.organization.dto.OrganizationMemberResponse;
import java.util.List;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class OrganizationAdminService {

    private final CurrentOrganizationResolver currentOrganizationResolver;
    private final KeycloakAdminClient keycloakAdminClient;
    private final OrganizationMemberMapper organizationMemberMapper;

    public OrganizationAdminService(
            CurrentOrganizationResolver currentOrganizationResolver,
            KeycloakAdminClient keycloakAdminClient,
            OrganizationMemberMapper organizationMemberMapper) {
        this.currentOrganizationResolver = currentOrganizationResolver;
        this.keycloakAdminClient = keycloakAdminClient;
        this.organizationMemberMapper = organizationMemberMapper;
    }

    public List<OrganizationMemberResponse> listMembers() {
        String organizationId = resolveOrganizationKeycloakId();
        return keycloakAdminClient.listMembers(organizationId).stream()
                .map(organizationMemberMapper::toResponse)
                .toList();
    }

    public void addMember(AddOrganizationMemberRequest request) {
        String organizationId = resolveOrganizationKeycloakId();
        KeycloakUserSummary existingUser = keycloakAdminClient.findUserByEmail(request.getEmail());
        if (existingUser != null) {
            keycloakAdminClient.addExistingMember(organizationId, existingUser.id());
            return;
        }

        if (request.getFirstName() == null || request.getFirstName().isBlank()
                || request.getLastName() == null || request.getLastName().isBlank()) {
            throw new NewMemberNameRequiredException();
        }
        keycloakAdminClient.inviteNewMember(organizationId, request.getEmail(), request.getFirstName(), request.getLastName());
    }

    public void removeMember(String userId) {
        if (userId.equals(currentUserId())) {
            throw new SelfRemovalNotAllowedException();
        }
        String organizationId = resolveOrganizationKeycloakId();
        keycloakAdminClient.removeMember(organizationId, userId);
        keycloakAdminClient.logoutUser(userId);
    }

    private String resolveOrganizationKeycloakId() {
        String organizationAlias = currentOrganizationResolver.requireCurrentOrganizationId();
        return keycloakAdminClient.findOrganizationIdByAlias(organizationAlias);
    }

    private String currentUserId() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getSubject();
        }
        return null;
    }
}
