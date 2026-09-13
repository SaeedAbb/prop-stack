package com.propstack.property;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @NotBlank
    private String address;

    @Enumerated(EnumType.STRING)
    private PropertyType type;

    @Enumerated(EnumType.STRING)
    private PropertyStatus status;

    /** The owning Keycloak organization's alias (e.g. "example-org") - fixed at creation. */
    @NotBlank
    @Column(name = "organization_id", nullable = false, updatable = false)
    private String organizationId;
}
