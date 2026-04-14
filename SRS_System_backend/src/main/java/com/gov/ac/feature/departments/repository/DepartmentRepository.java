package com.gov.ac.feature.departments.repository;

import com.gov.ac.feature.departments.entity.DepartmentEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<DepartmentEntity, Long> {

  Optional<DepartmentEntity> findByIdAndDeletedAtIsNull(Long id);

  @EntityGraph(attributePaths = "parent")
  List<DepartmentEntity> findByDeletedAtIsNullAndActiveTrueOrderBySortOrderAsc();

  boolean existsByCodeIgnoreCaseAndDeletedAtIsNull(String code);

  boolean existsByCodeIgnoreCaseAndDeletedAtIsNullAndIdNot(String code, Long id);

  List<DepartmentEntity> findByParent_IdAndDeletedAtIsNull(Long parentId);
}
