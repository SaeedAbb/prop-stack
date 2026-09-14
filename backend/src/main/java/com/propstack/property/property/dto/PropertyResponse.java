package com.propstack.property.property.dto;

import com.propstack.property.address.dto.AddressResponse;
import com.propstack.property.property.persistence.PropertyStatus;
import com.propstack.property.property.persistence.PropertyType;
import java.time.Instant;
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
    private Instant deletedAt;
}
