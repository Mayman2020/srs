package com.gov.ac.feature.workflow.execution.mapper;

import com.gov.ac.feature.workflow.execution.dto.WorkflowTaskViewDto;
import java.time.Instant;
import java.util.Date;
import org.camunda.bpm.engine.task.Task;

public final class WorkflowTaskMapper {

  private WorkflowTaskMapper() {}

  public static WorkflowTaskViewDto toView(Task task) {
    Date created = task.getCreateTime();
    Instant createdInstant = created != null ? created.toInstant() : null;
    return new WorkflowTaskViewDto(
        task.getId(),
        task.getTaskDefinitionKey(),
        task.getName(),
        task.getAssignee(),
        task.getProcessInstanceId(),
        createdInstant);
  }
}
