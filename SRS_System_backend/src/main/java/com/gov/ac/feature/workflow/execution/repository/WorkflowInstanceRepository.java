package com.gov.ac.feature.workflow.execution.repository;

import com.gov.ac.feature.workflow.execution.entity.WorkflowInstanceEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstanceEntity, UUID> {

  List<WorkflowInstanceEntity> findByCorrespondence_IdAndDeletedAtIsNullOrderByStartedAtDesc(
      UUID correspondenceId);

  Optional<WorkflowInstanceEntity> findByProcessInstanceIdAndDeletedAtIsNull(
      String processInstanceId);

  /**
   * Average end-to-end routing time (seconds) per completion month, for workflow instances that
   * have an {@code ended_at}. {@code departmentId} is matched against the originator department
   * when not null; pass {@code null} for the global view.
   */
  @Query(
      value =
          "select date_trunc('month', wi.ended_at) as bucket, "
              + "round(avg(extract(epoch from (wi.ended_at - wi.started_at)))::numeric, 0) as avg_seconds, "
              + "count(*) as completed_count "
              + "from srs_system.workflow_instance wi "
              + "where wi.deleted_at is null "
              + "and wi.ended_at is not null "
              + "and wi.ended_at >= :fromInclusive "
              + "and wi.ended_at < :toExclusive "
              + "and (cast(:departmentId as bigint) is null "
              + "  or wi.originator_department_id = :departmentId "
              + "  or wi.target_department_id = :departmentId) "
              + "group by 1 order by 1",
      nativeQuery = true)
  List<Object[]> averageRoutingSecondsByMonth(
      @Param("fromInclusive") Instant fromInclusive,
      @Param("toExclusive") Instant toExclusive,
      @Param("departmentId") Long departmentId);
}
