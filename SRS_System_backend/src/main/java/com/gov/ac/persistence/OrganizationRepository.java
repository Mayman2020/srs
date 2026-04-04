package com.gov.ac.persistence;

import com.gov.ac.domain.org.Organization;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

  Optional<Organization> findByIdAndDeletedAtIsNull(Long id);
}
