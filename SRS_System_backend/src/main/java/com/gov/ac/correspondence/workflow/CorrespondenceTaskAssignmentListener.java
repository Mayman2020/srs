package com.gov.ac.correspondence.workflow;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * On user task {@code create}: assigns the first task to a specific user, to a role-based candidate
 * group, or defaults to {@link CorrespondenceWorkflowVariables#INITIATOR}.
 */
@Component("correspondenceTaskAssignmentListener")
@Slf4j
public class CorrespondenceTaskAssignmentListener implements TaskListener {

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
      return;
    }

    if (StringUtils.hasText(initiator)) {
      delegateTask.setAssignee(initiator.trim());
      log.debug(
          "Camunda task {} assigned to initiator {}", delegateTask.getId(), initiator.trim());
    } else {
      log.warn("Camunda task {}: no initiator variable; task left unassigned", delegateTask.getId());
    }
  }
}
