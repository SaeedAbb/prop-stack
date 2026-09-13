package com.propstack.property.service;

import com.propstack.organization.service.CurrentOrganizationResolver;
import com.propstack.property.dto.PropertyRequest;
import com.propstack.property.dto.PropertyResponse;
import com.propstack.property.exception.PropertyNotFoundException;
import com.propstack.property.persistence.Property;
import com.propstack.property.persistence.PropertyRepository;
import java.util.List;
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
        return propertyRepository.findAllByOrganizationId(organizationId).stream()
                .map(propertyMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PropertyResponse findById(Long id) {
        String organizationId = currentOrganizationResolver.requireCurrentOrganizationId();
        return propertyMapper.toResponse(getOrThrow(id, organizationId));
    }

    public PropertyResponse create(PropertyRequest request) {
        String organizationId = currentOrganizationResolver.requireCurrentOrganizationId();
        Property property = propertyMapper.toEntity(request);
        property.setOrganizationId(organizationId);
        return propertyMapper.toResponse(propertyRepository.save(property));
    }

    public PropertyResponse update(Long id, PropertyRequest request) {
        String organizationId = currentOrganizationResolver.requireCurrentOrganizationId();
        Property existing = getOrThrow(id, organizationId);
        propertyMapper.updateEntityFromRequest(request, existing);
        return propertyMapper.toResponse(propertyRepository.save(existing));
    }

    public void delete(Long id) {
        String organizationId = currentOrganizationResolver.requireCurrentOrganizationId();
        if (!propertyRepository.existsByIdAndOrganizationId(id, organizationId)) {
            throw new PropertyNotFoundException(id);
        }
        propertyRepository.deleteByIdAndOrganizationId(id, organizationId);
    }

    private Property getOrThrow(Long id, String organizationId) {
        return propertyRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new PropertyNotFoundException(id));
    }
}
