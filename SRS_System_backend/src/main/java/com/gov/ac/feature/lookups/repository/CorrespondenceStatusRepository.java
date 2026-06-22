package com.gov.ac.feature.lookups.repository;

import com.gov.ac.feature.lookups.entity.CorrespondenceStatusEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorrespondenceStatusRepository extends JpaRepository<CorrespondenceStatusEntity, Long> {

  @EntityGraph(attributePaths = "correspondenceType")
  List<CorrespondenceStatusEntity> findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc();

  @EntityGraph(attributePaths = "correspondenceType")
  List<CorrespondenceStatusEntity> findByDeletedAtIsNullOrderBySortOrderAsc();

  Optional<CorrespondenceStatusEntity> findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(String code);

  Optional<CorrespondenceStatusEntity> findByIdAndDeletedAtIsNull(Long id);

  boolean existsByCorrespondenceType_IdAndCodeIgnoreCaseAndDeletedAtIsNull(
      Long correspondenceTypeId, String code);

  boolean existsByCorrespondenceTypeIsNullAndCodeIgnoreCaseAndDeletedAtIsNull(String code);

  boolean existsByCorrespondenceType_IdAndCodeIgnoreCaseAndIdNotAndDeletedAtIsNull(
      Long correspondenceTypeId, String code, Long id);

  boolean existsByCorrespondenceTypeIsNullAndCodeIgnoreCaseAndIdNotAndDeletedAtIsNull(
      String code, Long id);

  /** The single lifecycle row that user cancel transitions into ({@code cancel_outcome} in Flyway V28). */
  Optional<CorrespondenceStatusEntity> findByCancelOutcomeTrueAndActiveTrueAndDeletedAtIsNull();

  Optional<CorrespondenceStatusEntity> findByProcessCompleteOutcomeTrueAndActiveTrueAndDeletedAtIsNull();
}
