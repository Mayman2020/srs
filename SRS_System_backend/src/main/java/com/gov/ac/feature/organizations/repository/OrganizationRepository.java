package com.gov.ac.feature.organizations.repository;

import com.gov.ac.feature.organizations.entity.OrganizationEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<OrganizationEntity, Long> {

  Optional<OrganizationEntity> findByIdAndDeletedAtIsNull(Long id);

  List<OrganizationEntity> findByDeletedAtIsNullOrderByIdAsc();
}
