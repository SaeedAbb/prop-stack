package com.propstack.property.property.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.propstack.organization.organization.service.CurrentOrganizationResolver;
import com.propstack.property.property.dto.PropertyRequest;
import com.propstack.property.property.dto.PropertyResponse;
import com.propstack.property.property.exception.PropertyNotFoundException;
import com.propstack.property.property.persistence.Property;
import com.propstack.property.property.persistence.PropertyRepository;
import com.propstack.property.property.persistence.PropertyStatus;
import com.propstack.property.property.persistence.PropertyType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PropertyServiceTest {

    private static final String ORGANIZATION_ID = "example-org";

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private PropertyMapper propertyMapper;

    @Mock
    private CurrentOrganizationResolver currentOrganizationResolver;

    private PropertyService propertyService;

    @BeforeEach
    void setUp() {
        propertyService = new PropertyService(propertyRepository, propertyMapper, currentOrganizationResolver);
        when(currentOrganizationResolver.requireCurrentOrganizationId()).thenReturn(ORGANIZATION_ID);
    }

    @Test
    void findAll_returnsPropertiesScopedToCurrentOrganization() {
        Property property = new Property(1L, "Sunset Apartments", null, PropertyType.APARTMENT, PropertyStatus.AVAILABLE, ORGANIZATION_ID);
        PropertyResponse response = PropertyResponse.builder().id(1L).name("Sunset Apartments").build();
        when(propertyRepository.findAllByOrganizationId(ORGANIZATION_ID)).thenReturn(List.of(property));
        when(propertyMapper.toResponse(property)).thenReturn(response);

        List<PropertyResponse> result = propertyService.findAll();

        assertThat(result).containsExactly(response);
    }

    @Test
    void findById_returnsMappedProperty_whenFound() {
        Property property = new Property();
        property.setId(5L);
        PropertyResponse response = PropertyResponse.builder().id(5L).build();
        when(propertyRepository.findByIdAndOrganizationId(5L, ORGANIZATION_ID)).thenReturn(Optional.of(property));
        when(propertyMapper.toResponse(property)).thenReturn(response);

        PropertyResponse result = propertyService.findById(5L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void findById_throwsNotFound_whenMissing() {
        when(propertyRepository.findByIdAndOrganizationId(99L, ORGANIZATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> propertyService.findById(99L))
                .isInstanceOf(PropertyNotFoundException.class);
    }

    @Test
    void create_setsOrganizationIdFromResolver_beforeSaving() {
        PropertyRequest request = PropertyRequest.builder().name("New Property").build();
        Property mappedEntity = new Property();
        Property savedEntity = new Property();
        savedEntity.setOrganizationId(ORGANIZATION_ID);
        PropertyResponse response = PropertyResponse.builder().organizationId(ORGANIZATION_ID).build();

        when(propertyMapper.toEntity(request)).thenReturn(mappedEntity);
        when(propertyRepository.save(mappedEntity)).thenReturn(savedEntity);
        when(propertyMapper.toResponse(savedEntity)).thenReturn(response);

        PropertyResponse result = propertyService.create(request);

        assertThat(mappedEntity.getOrganizationId()).isEqualTo(ORGANIZATION_ID);
        assertThat(result).isEqualTo(response);
    }

    @Test
    void update_throwsNotFound_whenMissing() {
        PropertyRequest request = PropertyRequest.builder().name("Updated").build();
        when(propertyRepository.findByIdAndOrganizationId(7L, ORGANIZATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> propertyService.update(7L, request))
                .isInstanceOf(PropertyNotFoundException.class);
    }

    @Test
    void update_appliesRequestToExistingEntity_andSaves() {
        PropertyRequest request = PropertyRequest.builder().name("Updated").build();
        Property existing = new Property();
        existing.setId(7L);
        PropertyResponse response = PropertyResponse.builder().id(7L).name("Updated").build();

        when(propertyRepository.findByIdAndOrganizationId(7L, ORGANIZATION_ID)).thenReturn(Optional.of(existing));
        when(propertyRepository.save(existing)).thenReturn(existing);
        when(propertyMapper.toResponse(existing)).thenReturn(response);

        PropertyResponse result = propertyService.update(7L, request);

        verify(propertyMapper).updateEntityFromRequest(request, existing);
        assertThat(result).isEqualTo(response);
    }

    @Test
    void delete_throwsNotFound_whenMissing() {
        when(propertyRepository.existsByIdAndOrganizationId(3L, ORGANIZATION_ID)).thenReturn(false);

        assertThatThrownBy(() -> propertyService.delete(3L))
                .isInstanceOf(PropertyNotFoundException.class);
    }

    @Test
    void delete_removesProperty_whenFound() {
        when(propertyRepository.existsByIdAndOrganizationId(3L, ORGANIZATION_ID)).thenReturn(true);

        propertyService.delete(3L);

        verify(propertyRepository).deleteByIdAndOrganizationId(3L, ORGANIZATION_ID);
    }
}
