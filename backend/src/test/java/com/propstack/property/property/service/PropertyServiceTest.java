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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    void findAll_returnsActivePropertiesScopedToCurrentOrganization() {
        Property property = new Property(1L, "Sunset Apartments", null, PropertyType.APARTMENT, PropertyStatus.AVAILABLE, ORGANIZATION_ID, null);
        PropertyResponse response = PropertyResponse.builder().id(1L).name("Sunset Apartments").build();
        when(propertyRepository.findAllByOrganizationIdAndDeletedAtIsNull(ORGANIZATION_ID)).thenReturn(List.of(property));
        when(propertyMapper.toResponse(property)).thenReturn(response);

        List<PropertyResponse> result = propertyService.findAll();

        assertThat(result).containsExactly(response);
    }

    @Test
    void findAllDeleted_returnsSoftDeletedPropertiesScopedToCurrentOrganization() {
        Property property = new Property(2L, "Old Warehouse", null, PropertyType.BUILDING, PropertyStatus.SOLD, ORGANIZATION_ID, Instant.now());
        PropertyResponse response = PropertyResponse.builder().id(2L).name("Old Warehouse").build();
        when(propertyRepository.findAllByOrganizationIdAndDeletedAtIsNotNull(ORGANIZATION_ID)).thenReturn(List.of(property));
        when(propertyMapper.toResponse(property)).thenReturn(response);

        List<PropertyResponse> result = propertyService.findAllDeleted();

        assertThat(result).containsExactly(response);
    }

    @Test
    void findById_returnsMappedProperty_whenFound() {
        Property property = new Property();
        property.setId(5L);
        PropertyResponse response = PropertyResponse.builder().id(5L).build();
        when(propertyRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(5L, ORGANIZATION_ID)).thenReturn(Optional.of(property));
        when(propertyMapper.toResponse(property)).thenReturn(response);

        PropertyResponse result = propertyService.findById(5L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void findById_throwsNotFound_whenMissing() {
        when(propertyRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(99L, ORGANIZATION_ID)).thenReturn(Optional.empty());

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
        when(propertyRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(7L, ORGANIZATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> propertyService.update(7L, request))
                .isInstanceOf(PropertyNotFoundException.class);
    }

    @Test
    void update_appliesRequestToExistingEntity_andSaves() {
        PropertyRequest request = PropertyRequest.builder().name("Updated").build();
        Property existing = new Property();
        existing.setId(7L);
        PropertyResponse response = PropertyResponse.builder().id(7L).name("Updated").build();

        when(propertyRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(7L, ORGANIZATION_ID)).thenReturn(Optional.of(existing));
        when(propertyRepository.save(existing)).thenReturn(existing);
        when(propertyMapper.toResponse(existing)).thenReturn(response);

        PropertyResponse result = propertyService.update(7L, request);

        verify(propertyMapper).updateEntityFromRequest(request, existing);
        assertThat(result).isEqualTo(response);
    }

    @Test
    void delete_throwsNotFound_whenMissing() {
        when(propertyRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(3L, ORGANIZATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> propertyService.delete(3L))
                .isInstanceOf(PropertyNotFoundException.class);
    }

    @Test
    void delete_softDeletesProperty_setsDeletedAtAndSaves() {
        Property existing = new Property();
        existing.setId(3L);
        when(propertyRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(3L, ORGANIZATION_ID)).thenReturn(Optional.of(existing));

        propertyService.delete(3L);

        ArgumentCaptor<Property> captor = ArgumentCaptor.forClass(Property.class);
        verify(propertyRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(3L);
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
    }

    @Test
    void restore_throwsNotFound_whenPropertyNotDeleted() {
        when(propertyRepository.findByIdAndOrganizationIdAndDeletedAtIsNotNull(4L, ORGANIZATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> propertyService.restore(4L))
                .isInstanceOf(PropertyNotFoundException.class);
    }

    @Test
    void restore_clearsDeletedAt_andSaves() {
        Property existing = new Property();
        existing.setId(4L);
        existing.setDeletedAt(Instant.now());
        PropertyResponse response = PropertyResponse.builder().id(4L).build();

        when(propertyRepository.findByIdAndOrganizationIdAndDeletedAtIsNotNull(4L, ORGANIZATION_ID)).thenReturn(Optional.of(existing));
        when(propertyRepository.save(existing)).thenReturn(existing);
        when(propertyMapper.toResponse(existing)).thenReturn(response);

        PropertyResponse result = propertyService.restore(4L);

        assertThat(existing.getDeletedAt()).isNull();
        assertThat(result).isEqualTo(response);
    }
}
