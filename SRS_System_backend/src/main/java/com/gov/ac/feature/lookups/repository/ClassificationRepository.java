package com.gov.ac.feature.lookups.repository;

import com.gov.ac.feature.lookups.entity.ClassificationEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassificationRepository extends JpaRepository<ClassificationEntity, Long> {

  Optional<ClassificationEntity> findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(String code);

  @EntityGraph(attributePaths = "parent")
  List<ClassificationEntity> findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc();

  @EntityGraph(attributePaths = "parent")
  List<ClassificationEntity> findByDeletedAtIsNullOrderBySortOrderAsc();

  Optional<ClassificationEntity> findByIdAndDeletedAtIsNull(Long id);

  boolean existsByParentIsNullAndCodeIgnoreCaseAndDeletedAtIsNull(String code);

  boolean existsByParent_IdAndCodeIgnoreCaseAndDeletedAtIsNull(Long parentId, String code);

  boolean existsByParentIsNullAndCodeIgnoreCaseAndIdNotAndDeletedAtIsNull(String code, Long id);

  boolean existsByParent_IdAndCodeIgnoreCaseAndIdNotAndDeletedAtIsNull(
      Long parentId, String code, Long id);
}
