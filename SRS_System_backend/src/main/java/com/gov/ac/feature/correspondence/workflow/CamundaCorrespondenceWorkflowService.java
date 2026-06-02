package com.gov.ac.feature.correspondence.workflow;

import com.gov.ac.feature.workflow.execution.service.WorkflowService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Starts Camunda processes. Uses {@link Propagation#MANDATORY} so engine calls always join the
 * caller's Spring transaction (same {@code PlatformTransactionManager} / datasource as JPA when
 * using Camunda Spring Boot starter), allowing rollback of persistence if startup fails.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CamundaCorrespondenceWorkflowService {

  private final WorkflowService workflowService;

  @Transactional(propagation = Propagation.MANDATORY)
  public StartedProcess startCorrespondenceProcess(
      String processDefinitionKey,
      String businessKey,
      UUID actorUserId,
      UUID correspondenceId,
      UUID wfFirstAssigneeUserId,
      String wfFirstCandidateGroup,
      Long originatorDepartmentId,
      Long targetDepartmentId) {
    Map<String, Object> variables = new HashMap<>();
    variables.put(CorrespondenceWorkflowVariables.INITIATOR, actorUserId.toString());
    variables.put(CorrespondenceWorkflowVariables.CORRESPONDENCE_ID, correspondenceId.toString());
    variables.put("referenceNumber", businessKey);
    if (wfFirstAssigneeUserId != null) {
      variables.put(
          CorrespondenceWorkflowVariables.WF_FIRST_ASSIGNEE_USER_ID,
          wfFirstAssigneeUserId.toString());
    }
    if (StringUtils.hasText(wfFirstCandidateGroup)) {
      variables.put(
          CorrespondenceWorkflowVariables.WF_FIRST_CANDIDATE_GROUP,
          wfFirstCandidateGroup.trim());
    }
    if (originatorDepartmentId != null) {
      variables.put(
          CorrespondenceWorkflowVariables.ORIGINATOR_DEPARTMENT_ID, originatorDepartmentId);
    }
    if (targetDepartmentId != null) {
      variables.put(CorrespondenceWorkflowVariables.TARGET_DEPARTMENT_ID, targetDepartmentId);
    }

    try {
      String processInstanceId =
          workflowService.startProcessByKey(processDefinitionKey, businessKey, variables);
      log.info(
          "Started Camunda process definitionKey={} processInstanceId={} businessKey={}",
          processDefinitionKey,
          processInstanceId,
          businessKey);
      return new StartedProcess(processDefinitionKey, processInstanceId);
    } catch (Exception ex) {
      log.error(
          "Failed to start Camunda process definitionKey={} businessKey={}",
          processDefinitionKey,
          businessKey,
          ex);
      throw ex;
    }
  }

  public record StartedProcess(String processDefinitionKey, String processInstanceId) {}
}
