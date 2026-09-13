package com.propstack.property.dto;

import com.propstack.property.persistence.PropertyStatus;
import com.propstack.property.persistence.PropertyType;
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
public class PropertyResponse {

    private Long id;
    private String name;
    private AddressResponse address;
    private PropertyType type;
    private PropertyStatus status;
    private String organizationId;
}
