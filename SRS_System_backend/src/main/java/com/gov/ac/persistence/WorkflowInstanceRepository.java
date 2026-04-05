package com.gov.ac.persistence;

import com.gov.ac.domain.workflow.WorkflowInstance;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, UUID> {

  List<WorkflowInstance> findByCorrespondence_IdAndDeletedAtIsNullOrderByStartedAtDesc(
      UUID correspondenceId);
}
