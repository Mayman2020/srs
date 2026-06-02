package com.gov.ac.feature.sla.service;

import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.feature.sla.dto.SlaTaskStatusDto;
import com.gov.ac.feature.sla.entity.SlaBreachEventEntity;
import com.gov.ac.feature.sla.entity.SlaPolicyEntity;
import com.gov.ac.feature.sla.repository.SlaBreachEventRepository;
import com.gov.ac.feature.workflow.execution.entity.WorkflowInstanceEntity;
import com.gov.ac.feature.workflow.execution.repository.WorkflowInstanceRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Per-task SLA status read endpoint. Computes the live status from:
 *
 * <ol>
 *   <li>Camunda task (createTime + assignee).
 *   <li>The applicable {@link SlaPolicyEntity} resolved by {@link SlaPolicyResolverService}.
 *   <li>The latest {@link SlaBreachEventEntity} row (if a breach has already been recorded).
 * </ol>
 *
 * <p>Used by the workflow inbox countdown chip and the correspondence-details SLA panel. Returns
 * an "empty" status (overdue=false, secondsRemaining=0, no policy) for tasks that have no policy
 * match or that have already completed.
 */
@Service
@RequiredArgsConstructor
public class SlaTaskStatusService {

  private final TaskService taskService;
  private final WorkflowInstanceRepository workflowInstanceRepository;
  private final CorrespondenceRepository correspondenceRepository;
  private final SlaPolicyResolverService slaPolicyResolverService;
  private final SlaBreachEventRepository slaBreachEventRepository;

  @Transactional(readOnly = true)
  public SlaTaskStatusDto getStatusForTask(String taskId) {
    if (taskId == null || taskId.isBlank()) {
      throw new NotFoundException("Task id is required");
    }
    Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
    if (task == null) {
      // Maybe it just completed; return any breach event ledger entry we still have.
      Optional<SlaBreachEventEntity> historic = slaBreachEventRepository.findByTaskId(taskId);
      return historic
          .map(b -> emptyFor(taskId, b))
          .orElseGet(() -> empty(taskId));
    }
    Instant createdAt =
        task.getCreateTime() != null ? task.getCreateTime().toInstant() : Instant.now();
    WorkflowInstanceEntity wi =
        task.getProcessInstanceId() == null
            ? null
            : workflowInstanceRepository
                .findByProcessInstanceIdAndDeletedAtIsNull(task.getProcessInstanceId())
                .orElse(null);
    CorrespondenceEntity correspondence =
        wi != null && wi.getCorrespondence() != null
            ? correspondenceRepository.findById(wi.getCorrespondence().getId()).orElse(null)
            : null;
    String orgLevelCode = wi != null ? wi.getCurrentLevelCode() : null;
    Optional<SlaPolicyEntity> policyOpt =
        slaPolicyResolverService.resolveFor(correspondence, orgLevelCode, null);
    if (policyOpt.isEmpty()) {
      return empty(taskId);
    }
    SlaPolicyEntity policy = policyOpt.get();
    Instant target =
        createdAt
            .plus(Duration.ofHours(policy.getTargetHours()))
            .plus(Duration.ofMinutes(policy.getBreachGraceMinutes() == null ? 0 : policy.getBreachGraceMinutes()));
    Instant now = Instant.now();
    boolean overdue = now.isAfter(target);
    Optional<SlaBreachEventEntity> breachOpt = slaBreachEventRepository.findByTaskId(taskId);

    long remaining = overdue ? 0 : Duration.between(now, target).getSeconds();
    long overdueBy = overdue ? Duration.between(target, now).getSeconds() : 0;
    Instant breachedAt = breachOpt.map(SlaBreachEventEntity::getBreachedAt).orElse(null);
    Instant resolvedAt = breachOpt.map(SlaBreachEventEntity::getResolvedAt).orElse(null);
    Integer lastOrder = breachOpt.map(SlaBreachEventEntity::getLastStepExecutedOrder).orElse(null);
    String lastAction = breachOpt.map(SlaBreachEventEntity::getLastStepActionCode).orElse(null);
    Integer stepsTotal = breachOpt.map(SlaBreachEventEntity::getStepsExecutedTotal).orElse(null);

    return new SlaTaskStatusDto(
        taskId,
        policy.getId(),
        policy.getCode(),
        createdAt,
        target,
        breachedAt,
        resolvedAt,
        overdue,
        remaining,
        overdueBy,
        lastOrder,
        lastAction,
        stepsTotal);
  }

  private SlaTaskStatusDto empty(String taskId) {
    return new SlaTaskStatusDto(
        taskId, null, null, null, null, null, null, false, 0, 0, null, null, null);
  }

  private SlaTaskStatusDto emptyFor(String taskId, SlaBreachEventEntity historic) {
    return new SlaTaskStatusDto(
        taskId,
        historic.getPolicy() != null ? historic.getPolicy().getId() : null,
        historic.getPolicy() != null ? historic.getPolicy().getCode() : null,
        null,
        historic.getTargetAt(),
        historic.getBreachedAt(),
        historic.getResolvedAt(),
        false,
        0,
        0,
        historic.getLastStepExecutedOrder(),
        historic.getLastStepActionCode(),
        historic.getStepsExecutedTotal());
  }
}
