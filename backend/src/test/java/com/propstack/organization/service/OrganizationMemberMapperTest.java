package com.propstack.organization.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.propstack.organization.client.KeycloakOrganizationMember;
import com.propstack.organization.dto.OrganizationMemberResponse;
import org.junit.jupiter.api.Test;

class OrganizationMemberMapperTest {

    private final OrganizationMemberMapper mapper = new OrganizationMemberMapperImpl();

    @Test
    void toResponse_mapsAllFields() {
        KeycloakOrganizationMember member =
                new KeycloakOrganizationMember("u1", "jdoe", "jdoe@example.com", "Jane", "Doe", true, "MANAGED");

        OrganizationMemberResponse response = mapper.toResponse(member);

        assertThat(response.getId()).isEqualTo("u1");
        assertThat(response.getUsername()).isEqualTo("jdoe");
        assertThat(response.getEmail()).isEqualTo("jdoe@example.com");
        assertThat(response.getFirstName()).isEqualTo("Jane");
        assertThat(response.getLastName()).isEqualTo("Doe");
        assertThat(response.isEnabled()).isTrue();
    }
}
