package com.propstack.property.property.service;

import com.propstack.organization.organization.service.CurrentOrganizationResolver;
import com.propstack.property.property.dto.PropertyRequest;
import com.propstack.property.property.dto.PropertyResponse;
import com.propstack.property.property.exception.PropertyNotFoundException;
import com.propstack.property.property.persistence.Property;
import com.propstack.property.property.persistence.PropertyRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final PropertyMapper propertyMapper;
    private final CurrentOrganizationResolver currentOrganizationResolver;

    public PropertyService(
            PropertyRepository propertyRepository,
            PropertyMapper propertyMapper,
            CurrentOrganizationResolver currentOrganizationResolver) {
        this.propertyRepository = propertyRepository;
        this.propertyMapper = propertyMapper;
        this.currentOrganizationResolver = currentOrganizationResolver;
    }

    @Transactional(readOnly = true)
    public List<PropertyResponse> findAll() {
        String organizationId = currentOrganizationResolver.requireCurrentOrganizationId();
        return propertyRepository.findAllByOrganizationIdAndDeletedAtIsNull(organizationId).stream()
                .map(propertyMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PropertyResponse> findAllDeleted() {
        String organizationId = currentOrganizationResolver.requireCurrentOrganizationId();
        return propertyRepository.findAllByOrganizationIdAndDeletedAtIsNotNull(organizationId).stream()
                .map(propertyMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PropertyResponse findById(UUID id) {
        String organizationId = currentOrganizationResolver.requireCurrentOrganizationId();
        return propertyMapper.toResponse(getOrThrow(id, organizationId));
    }

    public PropertyResponse create(PropertyRequest request) {
        String organizationId = currentOrganizationResolver.requireCurrentOrganizationId();
        Property property = propertyMapper.toEntity(request);
        property.setOrganizationId(organizationId);
        return propertyMapper.toResponse(propertyRepository.save(property));
    }

    public PropertyResponse update(UUID id, PropertyRequest request) {
        String organizationId = currentOrganizationResolver.requireCurrentOrganizationId();
        Property existing = getOrThrow(id, organizationId);
        propertyMapper.updateEntityFromRequest(request, existing);
        return propertyMapper.toResponse(propertyRepository.save(existing));
    }

    public void delete(UUID id) {
        String organizationId = currentOrganizationResolver.requireCurrentOrganizationId();
        Property existing = getOrThrow(id, organizationId);
        existing.setDeletedAt(Instant.now());
        propertyRepository.save(existing);
    }

    public PropertyResponse restore(UUID id) {
        String organizationId = currentOrganizationResolver.requireCurrentOrganizationId();
        Property existing = propertyRepository
                .findByIdAndOrganizationIdAndDeletedAtIsNotNull(id, organizationId)
                .orElseThrow(() -> new PropertyNotFoundException(id));
        existing.setDeletedAt(null);
        return propertyMapper.toResponse(propertyRepository.save(existing));
    }

    private Property getOrThrow(UUID id, String organizationId) {
        return propertyRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(id, organizationId)
                .orElseThrow(() -> new PropertyNotFoundException(id));
    }
}
