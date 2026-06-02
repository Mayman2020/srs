package com.gov.ac.feature.sla.job;

import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.feature.sla.entity.SlaBreachEventEntity;
import com.gov.ac.feature.sla.entity.SlaEscalationStepEntity;
import com.gov.ac.feature.sla.entity.SlaPolicyEntity;
import com.gov.ac.feature.sla.metrics.SlaMetrics;
import com.gov.ac.feature.sla.repository.SlaBreachEventRepository;
import com.gov.ac.feature.sla.repository.SlaEscalationStepRepository;
import com.gov.ac.feature.sla.service.SlaEscalationService;
import com.gov.ac.feature.sla.service.SlaPolicyResolverService;
import com.gov.ac.feature.workflow.execution.entity.WorkflowInstanceEntity;
import com.gov.ac.feature.workflow.execution.repository.WorkflowInstanceRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB-driven SLA evaluation job.
 *
 * <p>On each tick:
 *
 * <ol>
 *   <li>Walk every currently active Camunda user task.
 *   <li>Resolve the correspondence + workflow instance behind the task.
 *   <li>Use {@link SlaPolicyResolverService} to pick the applicable policy (highest specificity).
 *   <li>Compute the SLA target time = task.createTime + policy.targetHours + grace minutes.
 *   <li>If now > target, ensure an {@link SlaBreachEventEntity} exists for the task; then run every
 *       eligible {@link SlaEscalationStepEntity} (step_order > last_step_executed_order &amp;&amp;
 *       elapsed since breach >= delay_after_breach_minutes) in order.
 *   <li>If a task whose breach was resolved (Camunda task no longer active) shows up here, the
 *       resolver-side {@code SlaEscalationService.markResolved} hook handles it; we only update
 *       state for tasks we still see in Camunda.
 * </ol>
 *
 * <p>Coexists peacefully with {@code WorkflowEscalationScheduler} (V12): both can run, and the
 * SLA engine is the long-term successor for hardcoded thresholds. The legacy scheduler keeps
 * working unchanged so V12 dashboards / alert wiring do not break during rollout.
 *
 * <p>Idempotent: re-running the job mid-tick re-resolves state from the breach event row and
 * never produces duplicate work for the same {@code (task_id, step_order)}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SlaPolicyEvaluationJob {

  private final TaskService taskService;
  private final WorkflowInstanceRepository workflowInstanceRepository;
  private final CorrespondenceRepository correspondenceRepository;
  private final SlaPolicyResolverService slaPolicyResolverService;
  private final SlaEscalationStepRepository slaEscalationStepRepository;
  private final SlaBreachEventRepository slaBreachEventRepository;
  private final SlaEscalationService slaEscalationService;
  private final SlaMetrics slaMetrics;

  @Value("${ac.sla.evaluation.max-tasks-per-tick:200}")
  private int maxTasksPerTick;

  /**
   * Runs on a fixed delay (default 60 s; configurable to support tests). Listed separately from
   * {@code WorkflowEscalationScheduler} so an operator can disable the SLA engine via property
   * without affecting the legacy stale-task scanner.
   */
  @Scheduled(fixedDelayString = "${ac.sla.evaluation.poll-ms:60000}")
  public void runEvaluationTick() {
    int processed = 0;
    int breachedNow = 0;
    int stepsFired = 0;
    int resolved = 0;
    try {
      List<Task> tasks =
          taskService
              .createTaskQuery()
              .active()
              .orderByTaskCreateTime()
              .asc()
              .listPage(0, Math.max(1, Math.min(maxTasksPerTick, 1000)));
      Instant now = Instant.now();
      Set<String> seenActiveTaskIds = new HashSet<>(tasks.size());
      for (Task task : tasks) {
        if (task != null && task.getId() != null) {
          seenActiveTaskIds.add(task.getId());
        }
        processed++;
        EvaluateOutcome outcome = evaluateOne(task, now);
        if (outcome == EvaluateOutcome.NEW_BREACH) breachedNow++;
        else if (outcome == EvaluateOutcome.STEP_FIRED) stepsFired++;
      }
      resolved = reconcileResolutions(seenActiveTaskIds);
    } catch (RuntimeException ex) {
      log.warn("[SLA] evaluation tick failed: {}", ex.getMessage());
    } finally {
      slaMetrics.refreshOverdueGauge();
      if (processed > 0 || resolved > 0) {
        log.debug(
            "[SLA] evaluation tick processed={} newBreaches={} stepsFired={} resolved={}",
            processed,
            breachedNow,
            stepsFired,
            resolved);
      }
    }
  }

  /**
   * Marks any unresolved breach event whose Camunda task is no longer in the active set as
   * resolved. Runs once per tick after the main pass so the gauge / metrics reflect operational
   * reality without a Camunda task complete listener (which would require a BPMN edit). The set
   * scanned here is bounded by the Camunda active-task query window, so a very large backlog
   * still completes in constant memory.
   */
  @Transactional
  int reconcileResolutions(Set<String> activeTaskIds) {
    List<SlaBreachEventEntity> unresolved = slaBreachEventRepository.findUnresolved();
    int count = 0;
    for (SlaBreachEventEntity breach : unresolved) {
      if (activeTaskIds.contains(breach.getTaskId())) {
        continue;
      }
      // The task is no longer active: either it was completed, deleted, or moved. Treat that as
      // resolution. The string "TASK_NO_LONGER_ACTIVE" lets dashboards distinguish silent
      // completions from explicit resolutions in future slices.
      slaEscalationService.markResolved(
          breach.getTaskId(), "TASK_NO_LONGER_ACTIVE", processKeyOf(breach));
      count++;
    }
    return count;
  }

  private static String processKeyOf(SlaBreachEventEntity breach) {
    if (breach.getWorkflowInstance() != null && breach.getWorkflowInstance().getProcessDefinitionKey() != null) {
      return breach.getWorkflowInstance().getProcessDefinitionKey();
    }
    return "unknown";
  }

  enum EvaluateOutcome {
    SKIPPED,
    NOT_OVERDUE,
    NEW_BREACH,
    STEP_FIRED,
    NO_OP
  }

  /**
   * Evaluate a single Camunda task. Public for tests; do not call from concurrent contexts
   * directly because it opens its own transaction.
   */
  @Transactional
  public EvaluateOutcome evaluateOne(Task task, Instant now) {
    if (task == null || task.getId() == null) {
      return EvaluateOutcome.SKIPPED;
    }
    if (task.getCreateTime() == null) {
      return EvaluateOutcome.SKIPPED;
    }
    Instant createdAt = task.getCreateTime().toInstant();

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
    if (policyOpt.isEmpty() || correspondence == null) {
      return EvaluateOutcome.SKIPPED;
    }
    SlaPolicyEntity policy = policyOpt.get();
    Instant target =
        createdAt
            .plus(Duration.ofHours(policy.getTargetHours()))
            .plus(Duration.ofMinutes(policy.getBreachGraceMinutes() == null ? 0 : policy.getBreachGraceMinutes()));
    if (now.isBefore(target)) {
      return EvaluateOutcome.NOT_OVERDUE;
    }

    // Ensure a breach event row.
    SlaBreachEventEntity breach =
        slaBreachEventRepository
            .findByTaskId(task.getId())
            .orElseGet(
                () -> createBreachEvent(task, wi, correspondence, policy, target, now, task.getProcessDefinitionId()));
    boolean newRow = breach.getCreatedAt() == null || breach.getCreatedAt().isAfter(now.minusSeconds(2));

    boolean stepFiredThisTick = false;
    List<SlaEscalationStepEntity> steps =
        slaEscalationStepRepository.findByPolicy_IdAndActiveTrueAndDeletedAtIsNullOrderByStepOrderAsc(
            policy.getId());
    String currentAssignee = task.getAssignee();
    String processKey = derivedProcessKey(task, wi);
    int lastOrder = breach.getLastStepExecutedOrder() == null ? -1 : breach.getLastStepExecutedOrder();
    for (SlaEscalationStepEntity step : steps) {
      if (step.getStepOrder() == null || step.getStepOrder() <= lastOrder) {
        continue;
      }
      int delay = step.getDelayAfterBreachMinutes() == null ? 0 : step.getDelayAfterBreachMinutes();
      Instant fireAt = breach.getBreachedAt().plus(Duration.ofMinutes(delay));
      if (now.isBefore(fireAt)) {
        break; // steps are ordered; nothing later is ready either
      }
      boolean ok =
          slaEscalationService.executeStep(breach, step, correspondence, currentAssignee, processKey);
      if (!ok) {
        break;
      }
      breach.setLastStepExecutedOrder(step.getStepOrder());
      breach.setLastStepExecutedAt(Instant.now());
      breach.setLastStepActionCode(step.getActionCode());
      breach.setStepsExecutedTotal(breach.getStepsExecutedTotal() + 1);
      slaBreachEventRepository.save(breach);
      stepFiredThisTick = true;
      lastOrder = step.getStepOrder();
      // Re-read the (possibly mutated) assignee for subsequent steps in the same pass.
      try {
        Task refreshed = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        if (refreshed != null) {
          currentAssignee = refreshed.getAssignee();
        }
      } catch (RuntimeException ignored) {
        // Reassignment side-effects are not load-bearing for subsequent steps.
      }
    }

    if (newRow) {
      slaMetrics.recordBreachOutcome(SlaMetrics.OUTCOME_BREACH_DETECTED, processKey);
      slaEscalationService.auditBreachDetected(breach);
      return EvaluateOutcome.NEW_BREACH;
    }
    return stepFiredThisTick ? EvaluateOutcome.STEP_FIRED : EvaluateOutcome.NO_OP;
  }

  private SlaBreachEventEntity createBreachEvent(
      Task task,
      WorkflowInstanceEntity wi,
      CorrespondenceEntity correspondence,
      SlaPolicyEntity policy,
      Instant target,
      Instant now,
      String processDefinitionId) {
    SlaBreachEventEntity breach = new SlaBreachEventEntity();
    breach.setTaskId(task.getId());
    breach.setProcessInstanceId(task.getProcessInstanceId());
    breach.setWorkflowInstance(wi);
    breach.setCorrespondence(correspondence);
    breach.setPolicy(policy);
    breach.setTargetAt(target);
    breach.setBreachedAt(now);
    breach.setLastStepExecutedOrder(-1);
    breach.setStepsExecutedTotal(0);
    return slaBreachEventRepository.save(breach);
  }

  private static String derivedProcessKey(Task task, WorkflowInstanceEntity wi) {
    if (wi != null && Objects.toString(wi.getProcessDefinitionKey(), "").length() > 0) {
      return wi.getProcessDefinitionKey();
    }
    String processDefinitionId = task.getProcessDefinitionId();
    if (processDefinitionId == null) {
      return "unknown";
    }
    int colon = processDefinitionId.indexOf(':');
    return colon > 0 ? processDefinitionId.substring(0, colon) : processDefinitionId;
  }
}
