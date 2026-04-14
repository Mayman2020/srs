package com.gov.ac.feature.roles.repository;

import com.gov.ac.feature.roles.entity.PermissionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<PermissionEntity, Long> {

  List<PermissionEntity> findByDeletedAtIsNullOrderBySortOrderAsc();

  Optional<PermissionEntity> findByIdAndDeletedAtIsNull(Long id);

  Optional<PermissionEntity> findByCodeIgnoreCaseAndDeletedAtIsNull(String code);

  boolean existsByCodeIgnoreCaseAndDeletedAtIsNull(String code);

  boolean existsByCodeIgnoreCaseAndDeletedAtIsNullAndIdNot(String code, Long id);
}
