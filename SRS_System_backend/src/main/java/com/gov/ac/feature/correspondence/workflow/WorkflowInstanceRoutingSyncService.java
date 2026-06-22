package com.gov.ac.feature.correspondence.workflow;

import com.gov.ac.feature.workflow.execution.entity.WorkflowInstanceEntity;
import com.gov.ac.feature.workflow.execution.repository.WorkflowInstanceRepository;
import com.gov.ac.feature.workflow.execution.service.WorkflowService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Re-reads Camunda process variables after {@code workflow_instance} is persisted so routing chain
 * JSON is mirrored even when the delegate ran before the bridge row existed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowInstanceRoutingSyncService {

  private final WorkflowService workflowService;
  private final WorkflowInstanceRepository workflowInstanceRepository;

  @Transactional
  public void syncFromEngine(String processInstanceId) {
    if (processInstanceId == null || processInstanceId.isBlank()) {
      return;
    }
    workflowInstanceRepository
        .findByProcessInstanceIdAndDeletedAtIsNull(processInstanceId)
        .ifPresent(
            wi -> {
              workflowService
                  .getProcessVariable(processInstanceId, CorrespondenceWorkflowVariables.ROUTING_CHAIN_JSON)
                  .map(Object::toString)
                  .ifPresent(wi::setRoutingChainJson);

              workflowService
                  .getProcessVariable(processInstanceId, CorrespondenceWorkflowVariables.ORIGINATOR_DEPARTMENT_ID)
                  .ifPresent(v -> wi.setOriginatorDepartmentId(asLong(v)));

              workflowService
                  .getProcessVariable(processInstanceId, CorrespondenceWorkflowVariables.TARGET_DEPARTMENT_ID)
                  .ifPresent(v -> wi.setTargetDepartmentId(asLong(v)));

              @SuppressWarnings("unchecked")
              List<Map<String, Object>> stops =
                  (List<Map<String, Object>>)
                      workflowService
                          .getProcessVariable(
                              processInstanceId, CorrespondenceWorkflowVariables.ROUTING_STOPS)
                          .orElse(null);
              if (stops != null && !stops.isEmpty()) {
                applyFirstStop(wi, stops.get(0));
              }
              workflowInstanceRepository.save(wi);
              log.debug("Synced routing snapshot for processInstanceId={}", processInstanceId);
            });
  }

  private static void applyFirstStop(WorkflowInstanceEntity wi, Map<String, Object> first) {
    Object level = first.get("levelCode");
    if (level != null) {
      wi.setCurrentLevelCode(level.toString());
    }
    Long deptId = asLong(first.get("departmentId"));
    if (deptId != null) {
      wi.setCurrentDepartmentId(deptId);
    }
  }

  private static Long asLong(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number n) {
      return n.longValue();
    }
    try {
      return Long.parseLong(value.toString().trim());
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
