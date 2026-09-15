package com.propstack.property.address.persistence;

import com.propstack.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Address extends BaseEntity {

    @NotBlank
    private String street;

    @NotBlank
    @Column(name = "house_number")
    private String houseNumber;

    @NotBlank
    private String city;

    @NotBlank
    private String state;

    @NotBlank
    @Column(name = "postal_code")
    private String postalCode;

    @NotBlank
    private String country;
}
