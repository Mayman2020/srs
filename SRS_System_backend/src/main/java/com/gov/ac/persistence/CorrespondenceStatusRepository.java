package com.gov.ac.persistence;

import com.gov.ac.domain.lookup.CorrespondenceStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorrespondenceStatusRepository extends JpaRepository<CorrespondenceStatus, Long> {

  @EntityGraph(attributePaths = "correspondenceType")
  List<CorrespondenceStatus> findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc();

  @EntityGraph(attributePaths = "correspondenceType")
  List<CorrespondenceStatus> findByDeletedAtIsNullOrderBySortOrderAsc();

  Optional<CorrespondenceStatus> findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(String code);

  Optional<CorrespondenceStatus> findByIdAndDeletedAtIsNull(Long id);

  boolean existsByCorrespondenceType_IdAndCodeIgnoreCaseAndDeletedAtIsNull(
      Long correspondenceTypeId, String code);

  boolean existsByCorrespondenceTypeIsNullAndCodeIgnoreCaseAndDeletedAtIsNull(String code);

  boolean existsByCorrespondenceType_IdAndCodeIgnoreCaseAndIdNotAndDeletedAtIsNull(
      Long correspondenceTypeId, String code, Long id);

  boolean existsByCorrespondenceTypeIsNullAndCodeIgnoreCaseAndIdNotAndDeletedAtIsNull(
      String code, Long id);

  /** The single lifecycle row that user cancel transitions into ({@code cancel_outcome} in Flyway V28). */
  Optional<CorrespondenceStatus> findByCancelOutcomeTrueAndActiveTrueAndDeletedAtIsNull();
}
