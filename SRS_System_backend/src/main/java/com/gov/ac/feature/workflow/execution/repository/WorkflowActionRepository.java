package com.gov.ac.feature.workflow.execution.repository;

import com.gov.ac.feature.workflow.execution.entity.WorkflowActionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowActionRepository extends JpaRepository<WorkflowActionEntity, Long> {

  List<WorkflowActionEntity> findByCorrespondence_IdAndDeletedAtIsNullOrderByIdAsc(
      UUID correspondenceId);
}
