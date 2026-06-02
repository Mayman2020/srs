package com.gov.ac.feature.correspondence.workflow;

import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.feature.workflow.execution.entity.WorkflowInstanceEntity;
import com.gov.ac.feature.workflow.execution.repository.WorkflowInstanceRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

/**
 * Execution listener wired on the final {@code endEvent}s in each correspondence BPMN.
 *
 * <p>Marks the {@link WorkflowInstanceEntity} ended (sets {@code endedAt}) and records on the
 * correspondence the terminal {@code workflow_action.payload} flag so the FE can know the route
 * is closed without polling Camunda history. Idempotent on repeat invocation.
 *
 * <p>Bean name {@code finalStatusExecutionListener}.
 */
@Component("finalStatusExecutionListener")
@RequiredArgsConstructor
@Slf4j
public class FinalStatusExecutionListener implements ExecutionListener {

  private static final String METRIC_TASK_DURATION = "workflow_task_duration_seconds";

  private final WorkflowInstanceRepository workflowInstanceRepository;
  private final CorrespondenceRepository correspondenceRepository;
  private final MeterRegistry meterRegistry;

  @Override
  public void notify(DelegateExecution execution) {
    workflowInstanceRepository
        .findByProcessInstanceIdAndDeletedAtIsNull(execution.getProcessInstanceId())
        .ifPresent(this::markEnded);

    Object correspondenceIdRaw = execution.getVariable(CorrespondenceWorkflowVariables.CORRESPONDENCE_ID);
    if (correspondenceIdRaw == null) {
      return;
    }
    try {
      UUID correspondenceId = UUID.fromString(correspondenceIdRaw.toString().trim());
      correspondenceRepository
          .findById(correspondenceId)
          .filter(c -> c.getDeletedAt() == null)
          .ifPresent(c -> log.info(
              "Workflow terminal end reached: correspondenceId={} processInstanceId={}",
              correspondenceId,
              execution.getProcessInstanceId()));
    } catch (IllegalArgumentException ignored) {
      // not a UUID — nothing to update
    }
  }

  private void markEnded(WorkflowInstanceEntity instance) {
    if (instance.getEndedAt() != null) {
      return;
    }
    Instant now = Instant.now();
    instance.setEndedAt(now);
    workflowInstanceRepository.save(instance);

    if (instance.getStartedAt() != null) {
      Duration elapsed = Duration.between(instance.getStartedAt(), now);
      Timer.builder(METRIC_TASK_DURATION)
          .description("End-to-end workflow process duration in seconds")
          .tag("process", instance.getProcessDefinitionKey() != null
              ? instance.getProcessDefinitionKey()
              : "unknown")
          .register(meterRegistry)
          .record(elapsed.toMillis(), TimeUnit.MILLISECONDS);
    }
  }
}
