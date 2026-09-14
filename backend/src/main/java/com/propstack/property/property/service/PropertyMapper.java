package com.propstack.property.property.service;

import com.propstack.property.address.service.AddressMapper;
import com.propstack.property.property.dto.PropertyRequest;
import com.propstack.property.property.dto.PropertyResponse;
import com.propstack.property.property.persistence.Property;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = AddressMapper.class)
public interface PropertyMapper {

    Property toEntity(PropertyRequest request);

    PropertyResponse toResponse(Property entity);

    void updateEntityFromRequest(PropertyRequest request, @MappingTarget Property entity);
}
