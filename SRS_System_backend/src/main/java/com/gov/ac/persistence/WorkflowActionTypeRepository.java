package com.gov.ac.persistence;

import com.gov.ac.domain.lookup.WorkflowActionType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowActionTypeRepository extends JpaRepository<WorkflowActionType, Long> {

  List<WorkflowActionType> findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc();

  Optional<WorkflowActionType> findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(String code);
}
