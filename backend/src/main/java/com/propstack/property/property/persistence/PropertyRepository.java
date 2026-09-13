package com.propstack.property.property.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    List<Property> findAllByOrganizationId(String organizationId);

    Optional<Property> findByIdAndOrganizationId(Long id, String organizationId);

    boolean existsByIdAndOrganizationId(Long id, String organizationId);

    void deleteByIdAndOrganizationId(Long id, String organizationId);
}
