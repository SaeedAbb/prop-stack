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
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PropertyServiceTest {

    private static final String ORGANIZATION_ID = "example-org";

    private static final UUID PROPERTY_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PROPERTY_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID PROPERTY_ID_3 = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID PROPERTY_ID_4 = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID PROPERTY_ID_5 = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID PROPERTY_ID_7 = UUID.fromString("00000000-0000-0000-0000-000000000007");
    private static final UUID PROPERTY_ID_99 = UUID.fromString("00000000-0000-0000-0000-000000000099");

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
        Property property = Property.builder()
                .id(PROPERTY_ID_1)
                .name("Sunset Apartments")
                .type(PropertyType.APARTMENT)
                .status(PropertyStatus.AVAILABLE)
                .organizationId(ORGANIZATION_ID)
                .build();
        PropertyResponse response = PropertyResponse.builder().id(PROPERTY_ID_1).name("Sunset Apartments").build();
        when(propertyRepository.findAllByOrganizationIdAndDeletedAtIsNull(ORGANIZATION_ID)).thenReturn(List.of(property));
        when(propertyMapper.toResponse(property)).thenReturn(response);

        List<PropertyResponse> result = propertyService.findAll();

        assertThat(result).containsExactly(response);
    }

    @Test
    void findAllDeleted_returnsSoftDeletedPropertiesScopedToCurrentOrganization() {
        Property property = Property.builder()
                .id(PROPERTY_ID_2)
                .name("Old Warehouse")
                .type(PropertyType.BUILDING)
                .status(PropertyStatus.SOLD)
                .organizationId(ORGANIZATION_ID)
                .deletedAt(Instant.now())
                .build();
        PropertyResponse response = PropertyResponse.builder().id(PROPERTY_ID_2).name("Old Warehouse").build();
        when(propertyRepository.findAllByOrganizationIdAndDeletedAtIsNotNull(ORGANIZATION_ID)).thenReturn(List.of(property));
        when(propertyMapper.toResponse(property)).thenReturn(response);

        List<PropertyResponse> result = propertyService.findAllDeleted();

        assertThat(result).containsExactly(response);
    }

    @Test
    void findById_returnsMappedProperty_whenFound() {
        Property property = new Property();
        property.setId(PROPERTY_ID_5);
        PropertyResponse response = PropertyResponse.builder().id(PROPERTY_ID_5).build();
        when(propertyRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(PROPERTY_ID_5, ORGANIZATION_ID)).thenReturn(Optional.of(property));
        when(propertyMapper.toResponse(property)).thenReturn(response);

        PropertyResponse result = propertyService.findById(PROPERTY_ID_5);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void findById_throwsNotFound_whenMissing() {
        when(propertyRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(PROPERTY_ID_99, ORGANIZATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> propertyService.findById(PROPERTY_ID_99))
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
        when(propertyRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(PROPERTY_ID_7, ORGANIZATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> propertyService.update(PROPERTY_ID_7, request))
                .isInstanceOf(PropertyNotFoundException.class);
    }

    @Test
    void update_appliesRequestToExistingEntity_andSaves() {
        PropertyRequest request = PropertyRequest.builder().name("Updated").build();
        Property existing = new Property();
        existing.setId(PROPERTY_ID_7);
        PropertyResponse response = PropertyResponse.builder().id(PROPERTY_ID_7).name("Updated").build();

        when(propertyRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(PROPERTY_ID_7, ORGANIZATION_ID)).thenReturn(Optional.of(existing));
        when(propertyRepository.save(existing)).thenReturn(existing);
        when(propertyMapper.toResponse(existing)).thenReturn(response);

        PropertyResponse result = propertyService.update(PROPERTY_ID_7, request);

        verify(propertyMapper).updateEntityFromRequest(request, existing);
        assertThat(result).isEqualTo(response);
    }

    @Test
    void delete_throwsNotFound_whenMissing() {
        when(propertyRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(PROPERTY_ID_3, ORGANIZATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> propertyService.delete(PROPERTY_ID_3))
                .isInstanceOf(PropertyNotFoundException.class);
    }

    @Test
    void delete_softDeletesProperty_setsDeletedAtAndSaves() {
        Property existing = new Property();
        existing.setId(PROPERTY_ID_3);
        when(propertyRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(PROPERTY_ID_3, ORGANIZATION_ID)).thenReturn(Optional.of(existing));

        propertyService.delete(PROPERTY_ID_3);

        ArgumentCaptor<Property> captor = ArgumentCaptor.forClass(Property.class);
        verify(propertyRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(PROPERTY_ID_3);
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
    }

    @Test
    void restore_throwsNotFound_whenPropertyNotDeleted() {
        when(propertyRepository.findByIdAndOrganizationIdAndDeletedAtIsNotNull(PROPERTY_ID_4, ORGANIZATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> propertyService.restore(PROPERTY_ID_4))
                .isInstanceOf(PropertyNotFoundException.class);
    }

    @Test
    void restore_clearsDeletedAt_andSaves() {
        Property existing = new Property();
        existing.setId(PROPERTY_ID_4);
        existing.setDeletedAt(Instant.now());
        PropertyResponse response = PropertyResponse.builder().id(PROPERTY_ID_4).build();

        when(propertyRepository.findByIdAndOrganizationIdAndDeletedAtIsNotNull(PROPERTY_ID_4, ORGANIZATION_ID)).thenReturn(Optional.of(existing));
        when(propertyRepository.save(existing)).thenReturn(existing);
        when(propertyMapper.toResponse(existing)).thenReturn(response);

        PropertyResponse result = propertyService.restore(PROPERTY_ID_4);

        assertThat(existing.getDeletedAt()).isNull();
        assertThat(result).isEqualTo(response);
    }
}
