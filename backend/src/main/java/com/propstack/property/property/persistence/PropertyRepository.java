package com.propstack.property.property.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    List<Property> findAllByOrganizationIdAndDeletedAtIsNull(String organizationId);

    List<Property> findAllByOrganizationIdAndDeletedAtIsNotNull(String organizationId);

    Optional<Property> findByIdAndOrganizationIdAndDeletedAtIsNull(Long id, String organizationId);

    Optional<Property> findByIdAndOrganizationIdAndDeletedAtIsNotNull(Long id, String organizationId);
}
