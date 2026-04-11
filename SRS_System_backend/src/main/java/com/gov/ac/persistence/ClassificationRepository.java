package com.gov.ac.persistence;

import com.gov.ac.domain.org.Classification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassificationRepository extends JpaRepository<Classification, Long> {

  Optional<Classification> findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(String code);

  @EntityGraph(attributePaths = "parent")
  List<Classification> findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc();

  @EntityGraph(attributePaths = "parent")
  List<Classification> findByDeletedAtIsNullOrderBySortOrderAsc();

  Optional<Classification> findByIdAndDeletedAtIsNull(Long id);

  boolean existsByParentIsNullAndCodeIgnoreCaseAndDeletedAtIsNull(String code);

  boolean existsByParent_IdAndCodeIgnoreCaseAndDeletedAtIsNull(Long parentId, String code);

  boolean existsByParentIsNullAndCodeIgnoreCaseAndIdNotAndDeletedAtIsNull(String code, Long id);

  boolean existsByParent_IdAndCodeIgnoreCaseAndIdNotAndDeletedAtIsNull(
      Long parentId, String code, Long id);
}
