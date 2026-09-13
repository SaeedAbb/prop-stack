package com.propstack.property.dto;

import com.propstack.property.PropertyStatus;
import com.propstack.property.PropertyType;
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
    private String address;
    private PropertyType type;
    private PropertyStatus status;
    private String organizationId;
}
