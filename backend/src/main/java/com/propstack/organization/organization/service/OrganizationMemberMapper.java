package com.propstack.organization.organization.service;

import com.propstack.organization.keycloak.KeycloakOrganizationMember;
import com.propstack.organization.organization.dto.OrganizationMemberResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrganizationMemberMapper {

    OrganizationMemberResponse toResponse(KeycloakOrganizationMember member);
}
