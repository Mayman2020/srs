package com.gov.ac.correspondence.workflow;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CamundaCorrespondenceWorkflowService {

  private final RuntimeService runtimeService;

  public StartedProcess startCorrespondenceProcess(
      String processDefinitionKey, String businessKey, UUID actorUserId, UUID correspondenceId) {
    Map<String, Object> variables = new HashMap<>();
    variables.put("initiator", actorUserId.toString());
    variables.put("correspondenceId", correspondenceId.toString());
    variables.put("referenceNumber", businessKey);

    try {
      ProcessInstance instance =
          runtimeService.startProcessInstanceByKey(processDefinitionKey, businessKey, variables);
      log.info(
          "Started Camunda process definitionKey={} processInstanceId={} businessKey={}",
          processDefinitionKey,
          instance.getId(),
          businessKey);
      return new StartedProcess(processDefinitionKey, instance.getId());
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
