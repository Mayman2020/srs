package com.gov.ac.feature.correspondence.security;

import com.gov.ac.feature.roles.repository.RoleRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class CorrespondenceWorkflowParticipationChecker {

  private static final String INITIATOR_VAR = "initiator";

  private final RuntimeService runtimeService;
  private final TaskService taskService;
  private final HistoryService historyService;
  private final RoleRepository roleRepository;

  /**
   * True if the user is process initiator (variable), task assignee/candidate, historic assignee, or
   * historic initiator — keyed by Camunda business key ({@code reference_number}).
   */
  public boolean isParticipating(UUID userId, String businessKey) {
    if (!StringUtils.hasText(businessKey)) {
      return false;
    }
    String uid = userId.toString();

    if (runtimeService
            .createProcessInstanceQuery()
            .processInstanceBusinessKey(businessKey)
            .variableValueEquals(INITIATOR_VAR, uid)
            .count()
        > 0) {
      return true;
    }

    if (taskService
            .createTaskQuery()
            .processInstanceBusinessKey(businessKey)
            .taskAssignee(uid)
            .count()
        > 0) {
      return true;
    }

    if (taskService
            .createTaskQuery()
            .processInstanceBusinessKey(businessKey)
            .taskCandidateUser(uid)
            .count()
        > 0) {
      return true;
    }

    List<String> roleCodes = roleRepository.findActiveRoleCodesByUserId(userId);
    for (String role : roleCodes) {
      if (taskService
              .createTaskQuery()
              .processInstanceBusinessKey(businessKey)
              .taskCandidateGroup(role)
              .count()
          > 0) {
        return true;
      }
    }

    if (historyService
            .createHistoricTaskInstanceQuery()
            .processInstanceBusinessKey(businessKey)
            .taskAssignee(uid)
            .count()
        > 0) {
      return true;
    }

    return hasHistoricInitiator(businessKey, uid);
  }

  private boolean hasHistoricInitiator(String businessKey, String uid) {
    List<HistoricProcessInstance> instances =
        historyService
            .createHistoricProcessInstanceQuery()
            .processInstanceBusinessKey(businessKey)
            .list();
    for (HistoricProcessInstance pi : instances) {
      HistoricVariableInstance var =
          historyService
              .createHistoricVariableInstanceQuery()
              .processInstanceId(pi.getId())
              .variableName(INITIATOR_VAR)
              .singleResult();
      if (var != null && uid.equals(String.valueOf(var.getValue()))) {
        return true;
      }
    }
    return false;
  }
}
