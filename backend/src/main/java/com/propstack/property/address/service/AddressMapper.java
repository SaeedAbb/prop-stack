package com.propstack.property.address.service;

import com.propstack.property.address.dto.AddressRequest;
import com.propstack.property.address.dto.AddressResponse;
import com.propstack.property.address.persistence.Address;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    Address toEntity(AddressRequest request);

    AddressResponse toResponse(Address entity);

    void updateEntityFromRequest(AddressRequest request, @MappingTarget Address entity);
}
