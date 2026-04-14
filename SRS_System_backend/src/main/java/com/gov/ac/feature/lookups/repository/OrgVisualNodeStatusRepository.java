package com.gov.ac.feature.lookups.repository;

import com.gov.ac.feature.lookups.entity.OrgVisualNodeStatusEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgVisualNodeStatusRepository extends JpaRepository<OrgVisualNodeStatusEntity, Long> {

  List<OrgVisualNodeStatusEntity> findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc();

  List<OrgVisualNodeStatusEntity> findByDeletedAtIsNullOrderBySortOrderAsc();

  Optional<OrgVisualNodeStatusEntity> findByIdAndDeletedAtIsNull(Long id);

  boolean existsByCodeIgnoreCaseAndDeletedAtIsNull(String code);

  boolean existsByCodeIgnoreCaseAndIdNotAndDeletedAtIsNull(String code, Long id);
}
