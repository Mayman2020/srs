package com.gov.ac.persistence;

import com.gov.ac.domain.lookup.WorkflowHistoryEventType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowHistoryEventTypeRepository extends JpaRepository<WorkflowHistoryEventType, Long> {

  List<WorkflowHistoryEventType> findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc();

  Optional<WorkflowHistoryEventType> findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(String code);
}
