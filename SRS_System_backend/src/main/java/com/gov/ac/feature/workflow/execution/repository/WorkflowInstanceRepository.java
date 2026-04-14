package com.gov.ac.feature.workflow.execution.repository;

import com.gov.ac.feature.workflow.execution.entity.WorkflowInstanceEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstanceEntity, UUID> {

  List<WorkflowInstanceEntity> findByCorrespondence_IdAndDeletedAtIsNullOrderByStartedAtDesc(
      UUID correspondenceId);
}
