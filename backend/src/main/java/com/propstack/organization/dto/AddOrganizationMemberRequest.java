package com.propstack.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddOrganizationMemberRequest {

    @NotBlank
    @Email
    private String email;

    /** Only required when {@link #email} does not match an existing Keycloak user. */
    private String firstName;

    /** Only required when {@link #email} does not match an existing Keycloak user. */
    private String lastName;
}
