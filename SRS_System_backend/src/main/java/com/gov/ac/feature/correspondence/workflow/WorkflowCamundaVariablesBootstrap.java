package com.gov.ac.feature.correspondence.workflow;

import com.gov.ac.feature.admin.service.SystemParameterService;
import com.gov.ac.feature.lookups.repository.WorkflowActionTypeRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

/**
 * Writes Camunda process variables derived from {@code workflow_action_type} and system parameters
 * so BPMN never hardcodes decision codes or SLA fallbacks.
 */
@Component
@RequiredArgsConstructor
public class WorkflowCamundaVariablesBootstrap {

  public static final String CHAIN_EXIT_DECISION_CODES = "chainExitDecisionCodes";
  public static final String REJECT_DECISION_CODES = "rejectDecisionCodes";
  public static final String RETURN_DECISION_CODES = "returnDecisionCodes";
  public static final String DEFAULT_SLA_ISO = "defaultSlaIso";

  private final WorkflowActionTypeRepository workflowActionTypeRepository;
  private final SystemParameterService systemParameterService;

  public void apply(DelegateExecution execution) {
    execution.setVariable(
        CHAIN_EXIT_DECISION_CODES,
        new ArrayList<>(workflowActionTypeRepository.findActiveCodesTerminatingRoutingChain()));
    execution.setVariable(
        REJECT_DECISION_CODES,
        new ArrayList<>(workflowActionTypeRepository.findActiveCodesWithNextStatusCode("REJECTED")));
    execution.setVariable(
        RETURN_DECISION_CODES,
        new ArrayList<>(workflowActionTypeRepository.findActiveCodesWithNextStatusCode("RETURNED")));
    execution.setVariable(DEFAULT_SLA_ISO, systemParameterService.getDefaultSlaIsoDuration());
  }
}
