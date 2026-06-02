package com.gov.ac.feature.correspondence.workflow;

import com.gov.ac.feature.delegation.task.workflow.TaskDelegationAssignmentResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * On user task {@code create}: assigns the first task to a specific user, to a role-based candidate
 * group, or defaults to {@link CorrespondenceWorkflowVariables#INITIATOR}. After the canonical
 * assignee is set, defers to {@link TaskDelegationAssignmentResolver} so an active task delegation
 * can rewire the task to a delegate (no BPMN edits required).
 */
@Component("correspondenceTaskAssignmentListener")
@RequiredArgsConstructor
@Slf4j
public class CorrespondenceTaskAssignmentListener implements TaskListener {

  private final TaskDelegationAssignmentResolver taskDelegationAssignmentResolver;

  @Override
  public void notify(DelegateTask delegateTask) {
    if (!TaskListener.EVENTNAME_CREATE.equals(delegateTask.getEventName())) {
      return;
    }
    String initiator = (String) delegateTask.getVariable(CorrespondenceWorkflowVariables.INITIATOR);
    String assigneeUserId =
        (String) delegateTask.getVariable(CorrespondenceWorkflowVariables.WF_FIRST_ASSIGNEE_USER_ID);
    String candidateGroup =
        (String) delegateTask.getVariable(CorrespondenceWorkflowVariables.WF_FIRST_CANDIDATE_GROUP);

    if (StringUtils.hasText(assigneeUserId)) {
      String uid = assigneeUserId.trim();
      delegateTask.setAssignee(uid);
      log.debug(
          "Camunda task {} assigned to user {} (explicit first assignee)",
          delegateTask.getId(),
          uid);
      taskDelegationAssignmentResolver.resolveAndApply(delegateTask, uid);
      return;
    }

    if (StringUtils.hasText(candidateGroup)) {
      String g = candidateGroup.trim();
      delegateTask.setAssignee(null);
      delegateTask.addCandidateGroup(g);
      log.debug(
          "Camunda task {} opened as candidate group {} (any member may claim)",
          delegateTask.getId(),
          g);
      // Candidate-group tasks have no individual assignee yet — delegation will be re-evaluated
      // on claim/assignment via the inbox query overlay.
      return;
    }

    if (StringUtils.hasText(initiator)) {
      String uid = initiator.trim();
      delegateTask.setAssignee(uid);
      log.debug("Camunda task {} assigned to initiator {}", delegateTask.getId(), uid);
      taskDelegationAssignmentResolver.resolveAndApply(delegateTask, uid);
    } else {
      log.warn("Camunda task {}: no initiator variable; task left unassigned", delegateTask.getId());
    }
  }
}
