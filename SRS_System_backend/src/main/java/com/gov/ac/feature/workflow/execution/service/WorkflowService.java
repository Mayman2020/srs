package com.gov.ac.feature.workflow.execution.service;

import com.gov.ac.feature.workflow.execution.dto.WorkflowTaskViewDto;
import com.gov.ac.feature.workflow.execution.mapper.WorkflowTaskMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Facade over Camunda {@link RuntimeService} and {@link TaskService}. Keeps workflow engine APIs
 * behind a single entry point so domain code does not scatter direct engine calls; business tables
 * {@code workflow_instance} / {@code workflow_history} remain the persistence model for SRS.
 */
@Service
@RequiredArgsConstructor
public class WorkflowService {

  private final RuntimeService runtimeService;
  private final TaskService taskService;

  @Transactional
  public String startProcessByKey(
      String processDefinitionKey, String businessKey, Map<String, Object> variables) {
    return runtimeService
        .startProcessInstanceByKey(processDefinitionKey, businessKey, variables)
        .getId();
  }

  @Transactional
  public void completeTask(String taskId, Map<String, Object> variables) {
    taskService.complete(taskId, variables);
  }

  @Transactional
  public void claimTask(String taskId, String userId) {
    taskService.claim(taskId, userId);
  }

  @Transactional
  public void delegateTask(String taskId, String userId) {
    taskService.delegateTask(taskId, userId);
  }

  @Transactional
  public void deleteProcessInstance(String processInstanceId, String reason) {
    runtimeService.deleteProcessInstance(processInstanceId, reason);
  }

  public Optional<Object> getProcessVariable(String processInstanceId, String name) {
    return Optional.ofNullable(runtimeService.getVariable(processInstanceId, name));
  }

  @Transactional
  public void setProcessVariable(String processInstanceId, String name, Object value) {
    runtimeService.setVariable(processInstanceId, name, value);
  }

  public Optional<String> findBusinessKey(String processInstanceId) {
    return Optional.ofNullable(
            runtimeService
                .createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult())
        .map(pi -> pi.getBusinessKey());
  }

  public boolean hasActiveProcessInstance(String processInstanceId) {
    return runtimeService
            .createProcessInstanceQuery()
            .processInstanceId(processInstanceId)
            .count()
        > 0;
  }

  /**
   * Active user tasks for a process instance, mapped to {@link WorkflowTaskViewDto}.
   */
  public List<WorkflowTaskViewDto> listActiveTasksForProcessInstance(String processInstanceId) {
    return taskService.createTaskQuery().processInstanceId(processInstanceId).active().list()
        .stream()
        .map(WorkflowTaskMapper::toView)
        .collect(Collectors.toList());
  }
}
