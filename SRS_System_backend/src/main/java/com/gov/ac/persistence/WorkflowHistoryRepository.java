package com.gov.ac.persistence;

import com.gov.ac.domain.workflow.WorkflowHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkflowHistoryRepository extends JpaRepository<WorkflowHistory, Long> {

  @EntityGraph(
      attributePaths = {
        "eventType",
        "workflowActionType",
        "workflowAction",
        "actor",
        "previousCorrespondenceStatus",
        "newCorrespondenceStatus",
        "priorityAtEvent"
      })
  List<WorkflowHistory> findByCorrespondence_IdOrderBySequenceNoAsc(UUID correspondenceId);

  @Query(
      "select coalesce(max(h.sequenceNo), 0) from WorkflowHistory h where h.correspondence.id = :correspondenceId")
  int maxSequenceNo(@Param("correspondenceId") UUID correspondenceId);
}
