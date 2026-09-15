package com.propstack.property.property.persistence;

import com.propstack.common.persistence.BaseEntity;
import com.propstack.property.address.persistence.Address;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Property extends BaseEntity {

    @NotBlank
    private String name;

    @NotNull
    @Valid
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "address_id", nullable = false, unique = true)
    private Address address;

    @Enumerated(EnumType.STRING)
    private PropertyType type;

    @Enumerated(EnumType.STRING)
    private PropertyStatus status;

    /** The owning Keycloak organization's alias (e.g. "example-org") - fixed at creation. */
    @NotBlank
    @Column(name = "organization_id", nullable = false, updatable = false)
    private String organizationId;

    /** Non-null when soft-deleted; null means the property is active ("Owned"). */
    private Instant deletedAt;
}
