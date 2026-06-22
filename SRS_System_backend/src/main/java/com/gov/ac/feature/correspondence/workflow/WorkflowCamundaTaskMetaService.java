package com.gov.ac.feature.correspondence.workflow;

import com.gov.ac.feature.correspondence.repository.WorkflowCamundaTaskMetaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class WorkflowCamundaTaskMetaService {

  private final WorkflowCamundaTaskMetaRepository workflowCamundaTaskMetaRepository;

  @Transactional(readOnly = true)
  public boolean advancesRoutingCursor(String taskDefinitionKey) {
    if (!StringUtils.hasText(taskDefinitionKey)) {
      return false;
    }
    return workflowCamundaTaskMetaRepository
        .findById(taskDefinitionKey.trim())
        .map(row -> Boolean.TRUE.equals(row.getAdvancesRoutingCursor()))
        .orElse(false);
  }
}
