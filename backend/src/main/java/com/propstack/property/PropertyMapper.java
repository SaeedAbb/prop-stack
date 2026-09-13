package com.propstack.property;

import com.propstack.property.dto.PropertyRequest;
import com.propstack.property.dto.PropertyResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PropertyMapper {

    Property toEntity(PropertyRequest request);

    PropertyResponse toResponse(Property entity);

    void updateEntityFromRequest(PropertyRequest request, @MappingTarget Property entity);
}
