package com.gov.ac.feature.sla.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Job-level behaviour: overdue transition, idempotency, and not-overdue short-circuit. The job is
 * the orchestrator on which the SLA story stands or falls, so we pin its essential states here
 * even though the bulk of the action lives in {@link SlaEscalationService}.
 */
@ExtendWith(MockitoExtension.class)
class SlaPolicyEvaluationJobTest {

  @Mock private TaskService taskService;
  @Mock private WorkflowInstanceRepository workflowInstanceRepository;
  @Mock private CorrespondenceRepository correspondenceRepository;
  @Mock private SlaPolicyResolverService slaPolicyResolverService;
  @Mock private SlaEscalationStepRepository slaEscalationStepRepository;
  @Mock private SlaBreachEventRepository slaBreachEventRepository;
  @Mock private SlaEscalationService slaEscalationService;
  @Mock private SlaMetrics slaMetrics;

  @InjectMocks private SlaPolicyEvaluationJob job;

  private SlaPolicyEntity policy;
  private CorrespondenceEntity correspondence;
  private WorkflowInstanceEntity wi;
  private Task task;

  @BeforeEach
  void setUp() {
    policy = new SlaPolicyEntity();
    policy.setId(1L);
    policy.setCode("SLA_DEFAULT");
    policy.setTargetHours(4);
    policy.setBreachGraceMinutes(0);
    policy.setActive(true);

    correspondence = new CorrespondenceEntity();
    correspondence.setId(UUID.randomUUID());
    correspondence.setReferenceNumber("REF-1");
    correspondence.setSubject("Subj");

    wi = new WorkflowInstanceEntity();
    wi.setProcessInstanceId("pi-1");
    wi.setProcessDefinitionKey("inbound-correspondence");
    wi.setCorrespondence(correspondence);

    task = mockTask("task-1", "pi-1", Date.from(Instant.now().minus(Duration.ofHours(6))));

    lenient()
        .when(workflowInstanceRepository.findByProcessInstanceIdAndDeletedAtIsNull("pi-1"))
        .thenReturn(Optional.of(wi));
    lenient()
        .when(correspondenceRepository.findById(correspondence.getId()))
        .thenReturn(Optional.of(correspondence));
    lenient()
        .when(slaPolicyResolverService.resolveFor(eq(correspondence), any(), any()))
        .thenReturn(Optional.of(policy));
  }

  @Test
  void createsBreachEventAndAdvancesFirstStepWhenOverdue() {
    SlaEscalationStepEntity step0 = step(policy, 0, "NOTIFY_MANAGER", 0);
    when(slaEscalationStepRepository
            .findByPolicy_IdAndActiveTrueAndDeletedAtIsNullOrderByStepOrderAsc(1L))
        .thenReturn(List.of(step0));
    when(slaBreachEventRepository.findByTaskId("task-1")).thenReturn(Optional.empty());
    when(slaBreachEventRepository.save(any()))
        .thenAnswer(
            inv -> {
              SlaBreachEventEntity e = inv.getArgument(0);
              if (e.getId() == null) e.setId(42L);
              return e;
            });
    when(slaEscalationService.executeStep(any(), eq(step0), eq(correspondence), any(), any()))
        .thenReturn(true);

    SlaPolicyEvaluationJob.EvaluateOutcome outcome = job.evaluateOne(task, Instant.now());

    assertThat(outcome).isEqualTo(SlaPolicyEvaluationJob.EvaluateOutcome.NEW_BREACH);
    verify(slaEscalationService).executeStep(any(), eq(step0), eq(correspondence), any(), any());
    verify(slaMetrics).recordBreachOutcome(eq(SlaMetrics.OUTCOME_BREACH_DETECTED), any());
  }

  @Test
  void doesNotFireStepsBeforeTargetTime() {
    task = mockTask("task-1", "pi-1", Date.from(Instant.now().minus(Duration.ofHours(1))));

    SlaPolicyEvaluationJob.EvaluateOutcome outcome = job.evaluateOne(task, Instant.now());

    assertThat(outcome).isEqualTo(SlaPolicyEvaluationJob.EvaluateOutcome.NOT_OVERDUE);
    verify(slaEscalationService, never()).executeStep(any(), any(), any(), any(), any());
  }

  @Test
  void existingBreachEventIsReusedNotDuplicated() {
    SlaEscalationStepEntity step0 = step(policy, 0, "NOTIFY_MANAGER", 0);
    SlaEscalationStepEntity step1 = step(policy, 1, "REASSIGN_TO_DELEGATE", 30);
    when(slaEscalationStepRepository
            .findByPolicy_IdAndActiveTrueAndDeletedAtIsNullOrderByStepOrderAsc(1L))
        .thenReturn(List.of(step0, step1));

    SlaBreachEventEntity existing = new SlaBreachEventEntity();
    existing.setId(99L);
    existing.setTaskId("task-1");
    existing.setCorrespondence(correspondence);
    existing.setPolicy(policy);
    existing.setBreachedAt(Instant.now().minus(Duration.ofMinutes(45)));
    existing.setTargetAt(Instant.now().minus(Duration.ofMinutes(45)));
    existing.setLastStepExecutedOrder(0); // step 0 already fired
    // Simulate an older row written by a previous evaluation tick. The job's "is this brand new?"
    // check looks at createdAt vs the tick instant, so we must set it past the 2-second skew.
    existing.setCreatedAt(Instant.now().minus(Duration.ofMinutes(45)));

    when(slaBreachEventRepository.findByTaskId("task-1")).thenReturn(Optional.of(existing));
    when(slaEscalationService.executeStep(any(), eq(step1), eq(correspondence), any(), any()))
        .thenReturn(true);

    SlaPolicyEvaluationJob.EvaluateOutcome outcome = job.evaluateOne(task, Instant.now());

    assertThat(outcome).isEqualTo(SlaPolicyEvaluationJob.EvaluateOutcome.STEP_FIRED);
    verify(slaEscalationService, never())
        .executeStep(any(), eq(step0), any(), any(), any()); // step 0 was already done
    verify(slaEscalationService).executeStep(any(), eq(step1), any(), any(), any());
  }

  // ---------------------------------------------------------------------------

  private static SlaEscalationStepEntity step(
      SlaPolicyEntity policy, int order, String action, int delayMinutes) {
    SlaEscalationStepEntity s = new SlaEscalationStepEntity();
    s.setId((long) (1000 + order));
    s.setPolicy(policy);
    s.setStepOrder(order);
    s.setActionCode(action);
    s.setDelayAfterBreachMinutes(delayMinutes);
    s.setActive(true);
    return s;
  }

  private static Task mockTask(String id, String pi, Date createTime) {
    Task t = org.mockito.Mockito.mock(Task.class);
    lenient().when(t.getId()).thenReturn(id);
    lenient().when(t.getProcessInstanceId()).thenReturn(pi);
    lenient().when(t.getCreateTime()).thenReturn(createTime);
    lenient().when(t.getProcessDefinitionId()).thenReturn("inbound-correspondence:1:abc");
    return t;
  }
}
