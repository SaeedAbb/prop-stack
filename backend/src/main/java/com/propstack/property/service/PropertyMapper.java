package com.propstack.property.service;

import com.propstack.property.dto.AddressRequest;
import com.propstack.property.dto.PropertyRequest;
import com.propstack.property.dto.PropertyResponse;
import com.propstack.property.persistence.Address;
import com.propstack.property.persistence.Property;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PropertyMapper {

    Property toEntity(PropertyRequest request);

    PropertyResponse toResponse(Property entity);

    void updateEntityFromRequest(PropertyRequest request, @MappingTarget Property entity);

    void updateAddressFromRequest(AddressRequest request, @MappingTarget Address address);
}
