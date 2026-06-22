package com.gov.ac.feature.correspondence.workflow;

import com.gov.ac.feature.correspondence.CorrespondenceLookupCodes;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.feature.lookups.entity.CorrespondenceStatusEntity;
import com.gov.ac.feature.lookups.entity.WorkflowActionTypeEntity;
import com.gov.ac.feature.lookups.entity.WorkflowInstanceStatusEntity;
import com.gov.ac.feature.lookups.repository.CorrespondenceStatusRepository;
import com.gov.ac.feature.lookups.repository.WorkflowActionTypeRepository;
import com.gov.ac.feature.lookups.repository.WorkflowInstanceStatusRepository;
import com.gov.ac.feature.shared.lookup.service.LookupResolutionService;
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
import org.springframework.util.StringUtils;

/**
 * Execution listener on terminal BPMN end events. Terminal correspondence status is resolved from
 * {@code workflow_action_type} / {@code correspondence_status.process_complete_outcome} — not
 * hardcoded Java switches.
 */
@Component("finalStatusExecutionListener")
@RequiredArgsConstructor
@Slf4j
public class FinalStatusExecutionListener implements ExecutionListener {

  private static final String METRIC_TASK_DURATION = "workflow_task_duration_seconds";

  private final WorkflowInstanceRepository workflowInstanceRepository;
  private final WorkflowInstanceStatusRepository workflowInstanceStatusRepository;
  private final CorrespondenceRepository correspondenceRepository;
  private final CorrespondenceStatusRepository correspondenceStatusRepository;
  private final WorkflowActionTypeRepository workflowActionTypeRepository;
  private final LookupResolutionService lookups;
  private final MeterRegistry meterRegistry;

  @Override
  public void notify(DelegateExecution execution) {
    workflowInstanceRepository
        .findByProcessInstanceIdAndDeletedAtIsNull(execution.getProcessInstanceId())
        .ifPresent(this::markEnded);

    UUID correspondenceId = readCorrespondenceId(execution);
    if (correspondenceId == null) {
      return;
    }

    String terminalStatusCode = resolveTerminalStatusCode(execution);
    if (!StringUtils.hasText(terminalStatusCode)) {
      log.info(
          "Workflow terminal end reached without mapped status: correspondenceId={} activityId={}",
          correspondenceId,
          execution.getCurrentActivityId());
      return;
    }

    correspondenceRepository
        .findById(correspondenceId)
        .filter(c -> c.getDeletedAt() == null)
        .ifPresent(
            c -> {
              CorrespondenceStatusEntity terminal =
                  lookups.requireActiveCorrespondenceStatus(terminalStatusCode);
              if (c.getCorrespondenceStatus() == null
                  || !terminal.getId().equals(c.getCorrespondenceStatus().getId())) {
                c.setCorrespondenceStatus(terminal);
                correspondenceRepository.save(c);
              }
              log.info(
                  "Workflow terminal status applied: correspondenceId={} status={} processInstanceId={}",
                  correspondenceId,
                  terminalStatusCode,
                  execution.getProcessInstanceId());
            });
  }

  private String resolveTerminalStatusCode(DelegateExecution execution) {
    Object decisionRaw = execution.getVariable(CorrespondenceWorkflowVariables.WF_DECISION);
    if (decisionRaw != null && StringUtils.hasText(decisionRaw.toString())) {
      String decision = decisionRaw.toString().trim().toUpperCase();
      return workflowActionTypeRepository.findWildcardRulesForCode(decision).stream()
          .findFirst()
          .flatMap(this::terminalStatusFromActionRule)
          .orElseGet(() -> processCompleteOutcomeCode());
    }
    return processCompleteOutcomeCode();
  }

  private java.util.Optional<String> terminalStatusFromActionRule(WorkflowActionTypeEntity rule) {
    if (Boolean.TRUE.equals(rule.getSuppressProcessEndStatus())) {
      return java.util.Optional.empty();
    }
    CorrespondenceStatusEntity next = rule.getNextCorrespondenceStatus();
    if (next != null && Boolean.TRUE.equals(next.getTerminal())) {
      return java.util.Optional.of(next.getCode());
    }
    return java.util.Optional.empty();
  }

  private String processCompleteOutcomeCode() {
    return correspondenceStatusRepository
        .findByProcessCompleteOutcomeTrueAndActiveTrueAndDeletedAtIsNull()
        .map(CorrespondenceStatusEntity::getCode)
        .orElse(null);
  }

  private static UUID readCorrespondenceId(DelegateExecution execution) {
    Object correspondenceIdRaw = execution.getVariable(CorrespondenceWorkflowVariables.CORRESPONDENCE_ID);
    if (correspondenceIdRaw == null) {
      return null;
    }
    try {
      return UUID.fromString(correspondenceIdRaw.toString().trim());
    } catch (IllegalArgumentException ignored) {
      return null;
    }
  }

  private void markEnded(WorkflowInstanceEntity instance) {
    if (instance.getEndedAt() != null) {
      return;
    }
    Instant now = Instant.now();
    instance.setEndedAt(now);
    WorkflowInstanceStatusEntity completed =
        workflowInstanceStatusRepository
            .findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(
                CorrespondenceLookupCodes.WORKFLOW_INSTANCE_COMPLETED)
            .orElse(null);
    if (completed != null) {
      instance.setStatus(completed);
    }
    workflowInstanceRepository.save(instance);

    if (instance.getStartedAt() != null) {
      Duration elapsed = Duration.between(instance.getStartedAt(), now);
      Timer.builder(METRIC_TASK_DURATION)
          .description("End-to-end workflow process duration in seconds")
          .tag(
              "process",
              instance.getProcessDefinitionKey() != null
                  ? instance.getProcessDefinitionKey()
                  : "unknown")
          .register(meterRegistry)
          .record(elapsed.toMillis(), TimeUnit.MILLISECONDS);
    }
  }
}
