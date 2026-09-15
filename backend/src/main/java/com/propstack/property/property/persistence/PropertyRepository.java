package com.propstack.property.property.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyRepository extends JpaRepository<Property, UUID> {

    List<Property> findAllByOrganizationIdAndDeletedAtIsNull(String organizationId);

    List<Property> findAllByOrganizationIdAndDeletedAtIsNotNull(String organizationId);

    Optional<Property> findByIdAndOrganizationIdAndDeletedAtIsNull(UUID id, String organizationId);

    Optional<Property> findByIdAndOrganizationIdAndDeletedAtIsNotNull(UUID id, String organizationId);
}
