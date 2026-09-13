package com.propstack.property.property.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.propstack.property.address.dto.AddressRequest;
import com.propstack.property.address.persistence.Address;
import com.propstack.property.address.service.AddressMapperImpl;
import com.propstack.property.property.dto.PropertyRequest;
import com.propstack.property.property.dto.PropertyResponse;
import com.propstack.property.property.persistence.Property;
import com.propstack.property.property.persistence.PropertyStatus;
import com.propstack.property.property.persistence.PropertyType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PropertyMapperTest {

    private final PropertyMapper propertyMapper = newPropertyMapper();

    private static PropertyMapper newPropertyMapper() {
        PropertyMapperImpl mapper = new PropertyMapperImpl();
        ReflectionTestUtils.setField(mapper, "addressMapper", new AddressMapperImpl());
        return mapper;
    }

    @Test
    void toEntity_mapsAllFieldsIncludingNestedAddress() {
        PropertyRequest request = PropertyRequest.builder()
                .name("Skyline Tower")
                .address(AddressRequest.builder()
                        .street("Harbor Blvd")
                        .houseNumber("500")
                        .city("Baytown")
                        .state("CA")
                        .postalCode("94001")
                        .country("USA")
                        .build())
                .type(PropertyType.BUILDING)
                .status(PropertyStatus.AVAILABLE)
                .build();

        Property entity = propertyMapper.toEntity(request);

        assertThat(entity.getName()).isEqualTo("Skyline Tower");
        assertThat(entity.getType()).isEqualTo(PropertyType.BUILDING);
        assertThat(entity.getStatus()).isEqualTo(PropertyStatus.AVAILABLE);
        assertThat(entity.getAddress().getStreet()).isEqualTo("Harbor Blvd");
        assertThat(entity.getAddress().getHouseNumber()).isEqualTo("500");
        assertThat(entity.getAddress().getCity()).isEqualTo("Baytown");
    }

    @Test
    void toResponse_mapsAllFieldsIncludingNestedAddress() {
        Address address = new Address(1L, "Market Sq", "9", "Metropolis", "NY", "10001", "USA");
        Property entity = new Property(4L, "Downtown Plaza", address, PropertyType.COMMERCIAL, PropertyStatus.UNDER_MAINTENANCE, "example-org");

        PropertyResponse response = propertyMapper.toResponse(entity);

        assertThat(response.getId()).isEqualTo(4L);
        assertThat(response.getName()).isEqualTo("Downtown Plaza");
        assertThat(response.getOrganizationId()).isEqualTo("example-org");
        assertThat(response.getAddress().getStreet()).isEqualTo("Market Sq");
        assertThat(response.getAddress().getHouseNumber()).isEqualTo("9");
        assertThat(response.getAddress().getCountry()).isEqualTo("USA");
    }

    @Test
    void updateEntityFromRequest_updatesNestedAddressInPlace_ratherThanReplacingIt() {
        Address existingAddress = new Address(10L, "Old Street", "1", "Old City", "Old State", "00000", "Old Country");
        Property existing = new Property(2L, "Old Name", existingAddress, PropertyType.HOUSE, PropertyStatus.RENTED, "example-org");

        PropertyRequest request = PropertyRequest.builder()
                .name("New Name")
                .address(AddressRequest.builder()
                        .street("New Street")
                        .houseNumber("2")
                        .city("New City")
                        .state("New State")
                        .postalCode("11111")
                        .country("New Country")
                        .build())
                .type(PropertyType.APARTMENT)
                .status(PropertyStatus.SOLD)
                .build();

        propertyMapper.updateEntityFromRequest(request, existing);

        assertThat(existing.getName()).isEqualTo("New Name");
        assertThat(existing.getType()).isEqualTo(PropertyType.APARTMENT);
        assertThat(existing.getStatus()).isEqualTo(PropertyStatus.SOLD);
        // Regression guard: the Address row must be updated in place (same object/id), not
        // replaced with a new transient Address - otherwise, combined with orphanRemoval=true
        // on Property.address, Hibernate would delete+reinsert the address row on every update.
        assertThat(existing.getAddress()).isSameAs(existingAddress);
        assertThat(existing.getAddress().getId()).isEqualTo(10L);
        assertThat(existing.getAddress().getStreet()).isEqualTo("New Street");
        assertThat(existing.getAddress().getCity()).isEqualTo("New City");
    }
}
