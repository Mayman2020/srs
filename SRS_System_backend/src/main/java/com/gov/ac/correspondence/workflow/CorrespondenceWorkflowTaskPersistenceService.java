package com.gov.ac.correspondence.workflow;

import com.gov.ac.domain.correspondence.Correspondence;
import com.gov.ac.domain.lookup.CorrespondenceStatus;
import com.gov.ac.domain.lookup.WorkflowActionType;
import com.gov.ac.domain.lookup.WorkflowHistoryEventType;
import com.gov.ac.domain.user.AppUser;
import com.gov.ac.domain.workflow.WorkflowHistory;
import com.gov.ac.lookup.LookupResolutionService;
import com.gov.ac.persistence.AppUserRepository;
import com.gov.ac.persistence.CorrespondenceRepository;
import com.gov.ac.persistence.WorkflowHistoryRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Persists {@code workflow_history} and updates {@link Correspondence#getCorrespondenceStatus()}
 * when a Camunda user task completes. Expects process variables {@code correspondenceId}, optional
 * {@code wfDecision} ({@code APPROVE}|{@code REJECT}|{@code RETURN}), and optional {@code
 * actionComment}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CorrespondenceWorkflowTaskPersistenceService {

  public static final String VAR_CORRESPONDENCE_ID = "correspondenceId";
  public static final String VAR_WF_DECISION = "wfDecision";
  public static final String VAR_ACTION_COMMENT = "actionComment";

  private static final String EVENT_TASK_COMPLETED = "TASK_COMPLETED";

  private final CorrespondenceRepository correspondenceRepository;
  private final WorkflowHistoryRepository workflowHistoryRepository;
  private final LookupResolutionService lookups;
  private final AppUserRepository appUserRepository;

  @Transactional
  public void recordUserTaskCompleted(DelegateTask task) {
    String corrIdStr = (String) task.getVariable(VAR_CORRESPONDENCE_ID);
    if (!StringUtils.hasText(corrIdStr)) {
      log.warn("Workflow persistence skipped: missing correspondenceId on task {}", task.getId());
      return;
    }
    UUID correspondenceId;
    try {
      correspondenceId = UUID.fromString(corrIdStr.trim());
    } catch (IllegalArgumentException ex) {
      log.warn("Workflow persistence skipped: invalid correspondenceId={}", corrIdStr);
      return;
    }

    Correspondence correspondence =
        correspondenceRepository
            .findById(correspondenceId)
            .filter(c -> c.getDeletedAt() == null)
            .orElse(null);
    if (correspondence == null) {
      log.warn("Workflow persistence skipped: correspondence not found id={}", correspondenceId);
      return;
    }

    String decisionRaw = (String) task.getVariable(VAR_WF_DECISION);
    String decision =
        StringUtils.hasText(decisionRaw) ? decisionRaw.trim().toUpperCase() : "APPROVE";
    if (!decision.equals("APPROVE") && !decision.equals("REJECT") && !decision.equals("RETURN")) {
      log.warn("Workflow persistence: unknown wfDecision={}, defaulting to APPROVE", decisionRaw);
      decision = "APPROVE";
    }

    String comment = (String) task.getVariable(VAR_ACTION_COMMENT);
    String commentTrimmed = StringUtils.hasText(comment) ? comment.trim() : null;

    CorrespondenceStatus previous = correspondence.getCorrespondenceStatus();
    CorrespondenceStatus newStatus = resolveTargetStatus(decision);
    if (newStatus != null && !newStatus.getId().equals(previous.getId())) {
      correspondence.setCorrespondenceStatus(newStatus);
    }

    UUID actorId = null;
    if (StringUtils.hasText(task.getAssignee())) {
      try {
        actorId = UUID.fromString(task.getAssignee().trim());
      } catch (IllegalArgumentException ignored) {
        // non-UUID assignee
      }
    }
    if (actorId != null) {
      correspondence.setUpdatedBy(actorId);
    }
    correspondenceRepository.save(correspondence);

    AppUser actor =
        actorId != null
            ? appUserRepository.findByIdAndDeletedAtIsNull(actorId).orElse(null)
            : null;

    WorkflowHistoryEventType eventType = lookups.requireActiveHistoryEventType(EVENT_TASK_COMPLETED);
    WorkflowActionType actionType = lookups.requireActiveWorkflowActionType(decision);

    int nextSeq = workflowHistoryRepository.maxSequenceNo(correspondence.getId()) + 1;
    Instant now = Instant.now();

    WorkflowHistory history = new WorkflowHistory();
    history.setCorrespondence(correspondence);
    history.setEventType(eventType);
    history.setWorkflowActionType(actionType);
    history.setActor(actor);
    history.setOccurredAt(now);
    history.setSequenceNo(nextSeq);
    history.setPrimaryCommentText(commentTrimmed);
    history.setPreviousCorrespondenceStatus(previous);
    history.setNewCorrespondenceStatus(correspondence.getCorrespondenceStatus());
    history.setPriorityAtEvent(correspondence.getPriority());
    history.setCamundaTaskId(task.getId());
    history.setCamundaActivityId(task.getTaskDefinitionKey());
    Map<String, Object> detail = new HashMap<>();
    detail.put("taskDefinitionKey", task.getTaskDefinitionKey());
    detail.put("taskName", task.getName());
    detail.put("wfDecision", decision);
    history.setDetail(detail);
    if (actorId != null) {
      history.setCreatedBy(actorId);
      history.setUpdatedBy(actorId);
    }
    workflowHistoryRepository.save(history);

    log.info(
        "Recorded workflow task completion correspondenceId={} taskId={} decision={} seq={}",
        correspondenceId,
        task.getId(),
        decision,
        nextSeq);
  }

  private CorrespondenceStatus resolveTargetStatus(String decision) {
    String code =
        switch (decision) {
          case "REJECT" -> "REJECTED";
          case "RETURN" -> "RETURNED";
          default -> "COMPLETED";
        };
    return lookups.requireActiveCorrespondenceStatus(code);
  }
}
