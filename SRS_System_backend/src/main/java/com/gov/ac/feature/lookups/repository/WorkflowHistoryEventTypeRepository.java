package com.gov.ac.feature.lookups.repository;

import com.gov.ac.feature.lookups.entity.WorkflowHistoryEventTypeEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowHistoryEventTypeRepository extends JpaRepository<WorkflowHistoryEventTypeEntity, Long> {

  List<WorkflowHistoryEventTypeEntity> findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc();

  List<WorkflowHistoryEventTypeEntity> findByDeletedAtIsNullOrderBySortOrderAsc();

  Optional<WorkflowHistoryEventTypeEntity> findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(String code);

  Optional<WorkflowHistoryEventTypeEntity> findByIdAndDeletedAtIsNull(Long id);

  boolean existsByCodeIgnoreCaseAndDeletedAtIsNull(String code);

  boolean existsByCodeIgnoreCaseAndIdNotAndDeletedAtIsNull(String code, Long id);
}
