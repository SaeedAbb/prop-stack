package com.propstack.organization.service;

import com.propstack.organization.client.KeycloakOrganizationMember;
import com.propstack.organization.dto.OrganizationMemberResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrganizationMemberMapper {

    OrganizationMemberResponse toResponse(KeycloakOrganizationMember member);
}
