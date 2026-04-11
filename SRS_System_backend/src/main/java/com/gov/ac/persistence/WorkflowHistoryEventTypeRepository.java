package com.gov.ac.persistence;

import com.gov.ac.domain.lookup.WorkflowHistoryEventType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowHistoryEventTypeRepository extends JpaRepository<WorkflowHistoryEventType, Long> {

  List<WorkflowHistoryEventType> findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc();

  List<WorkflowHistoryEventType> findByDeletedAtIsNullOrderBySortOrderAsc();

  Optional<WorkflowHistoryEventType> findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(String code);

  Optional<WorkflowHistoryEventType> findByIdAndDeletedAtIsNull(Long id);

  boolean existsByCodeIgnoreCaseAndDeletedAtIsNull(String code);

  boolean existsByCodeIgnoreCaseAndIdNotAndDeletedAtIsNull(String code, Long id);
}
