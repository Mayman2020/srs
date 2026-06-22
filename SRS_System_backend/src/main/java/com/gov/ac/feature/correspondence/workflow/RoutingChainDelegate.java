package com.gov.ac.feature.correspondence.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.feature.organization.dto.RoutingChainDto;
import com.gov.ac.feature.organization.dto.RoutingStopDto;
import com.gov.ac.feature.organization.service.OrgRoutingService;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import com.gov.ac.feature.workflow.execution.entity.WorkflowInstanceEntity;
import com.gov.ac.feature.workflow.execution.repository.WorkflowInstanceRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Camunda {@link JavaDelegate} that runs once on process start (service task immediately after
 * the start event). Resolves the originator and target departments, calls {@link
 * OrgRoutingService} to compute the Q/L/K/S routing chain, and writes:
 *
 * <ul>
 *   <li>{@code routingChainJson} — full chain (audit) as JSON string
 *   <li>{@code routingStops} — list of stop maps, driver for the multi-instance subprocess
 *   <li>{@code workflow_instance.routing_chain_json / current_level_code / current_department_id}
 * </ul>
 *
 * <p>Registered with the bean name {@code routingChainDelegate} for use in BPMN via
 * {@code camunda:delegateExpression="${routingChainDelegate}"}.
 */
@Component("routingChainDelegate")
@RequiredArgsConstructor
@Slf4j
public class RoutingChainDelegate implements JavaDelegate {

  private final CorrespondenceRepository correspondenceRepository;
  private final AppUserRepository appUserRepository;
  private final OrgRoutingService orgRoutingService;
  private final WorkflowInstanceRepository workflowInstanceRepository;
  private final WorkflowSlaDurationResolver slaDurationResolver;
  private final WorkflowCamundaVariablesBootstrap camundaVariablesBootstrap;
  private final ObjectMapper objectMapper;

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    UUID correspondenceId = readCorrespondenceId(execution);
    if (correspondenceId == null) {
      throw new IllegalStateException(
          "routingChainDelegate: missing or invalid process variable "
              + CorrespondenceWorkflowVariables.CORRESPONDENCE_ID);
    }

    CorrespondenceEntity correspondence =
        correspondenceRepository
            .findById(correspondenceId)
            .filter(c -> c.getDeletedAt() == null)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "routingChainDelegate: correspondence not found id=" + correspondenceId));

    Long originatorDepartmentId =
        resolveOriginatorDepartmentId(execution, correspondence);
    Long targetDepartmentId = resolveTargetDepartmentId(execution, correspondence);

    if (originatorDepartmentId == null || targetDepartmentId == null) {
      log.warn(
          "routingChainDelegate: missing originatorDepartmentId={} or targetDepartmentId={} for correspondenceId={}; skipping chain",
          originatorDepartmentId,
          targetDepartmentId,
          correspondenceId);
      execution.setVariable(CorrespondenceWorkflowVariables.ROUTING_STOPS, List.of());
      execution.setVariable(CorrespondenceWorkflowVariables.ROUTING_CHAIN_JSON, "[]");
      camundaVariablesBootstrap.apply(execution);
      return;
    }

    RoutingChainDto chain =
        orgRoutingService.computeChain(originatorDepartmentId, targetDepartmentId);

    List<Map<String, Object>> stopMaps = toStopMaps(chain.stops(), correspondence);
    String chainJson = writeJson(stopMaps);

    execution.setVariable(CorrespondenceWorkflowVariables.ROUTING_STOPS, new ArrayList<>(stopMaps));
    execution.setVariable(CorrespondenceWorkflowVariables.ROUTING_CHAIN_JSON, chainJson);
    camundaVariablesBootstrap.apply(execution);

    // Mirror the snapshot on the bridge table so reports/dashboard can filter without parsing JSON.
    workflowInstanceRepository
        .findByProcessInstanceIdAndDeletedAtIsNull(execution.getProcessInstanceId())
        .ifPresent(
            (WorkflowInstanceEntity wi) -> {
              wi.setOriginatorDepartmentId(originatorDepartmentId);
              wi.setTargetDepartmentId(targetDepartmentId);
              wi.setRoutingChainJson(chainJson);
              if (!stopMaps.isEmpty()) {
                Map<String, Object> first = stopMaps.get(0);
                wi.setCurrentLevelCode((String) first.get("levelCode"));
                Object firstDeptId = first.get("departmentId");
                if (firstDeptId instanceof Number n) {
                  wi.setCurrentDepartmentId(n.longValue());
                }
              }
              workflowInstanceRepository.save(wi);
            });

    log.info(
        "routingChainDelegate: correspondenceId={} originator={} target={} stops={} reason={}",
        correspondenceId,
        originatorDepartmentId,
        targetDepartmentId,
        stopMaps.size(),
        chain.reasonKey());
  }

  private UUID readCorrespondenceId(DelegateExecution execution) {
    Object raw = execution.getVariable(CorrespondenceWorkflowVariables.CORRESPONDENCE_ID);
    if (raw == null) {
      return null;
    }
    try {
      return UUID.fromString(raw.toString().trim());
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private Long resolveOriginatorDepartmentId(
      DelegateExecution execution, CorrespondenceEntity correspondence) {
    Object explicit = execution.getVariable(CorrespondenceWorkflowVariables.ORIGINATOR_DEPARTMENT_ID);
    Long id = asLong(explicit);
    if (id != null) {
      return id;
    }
    // Fall back to the initiator user's department.
    Object initiator = execution.getVariable(CorrespondenceWorkflowVariables.INITIATOR);
    if (initiator != null) {
      try {
        AppUserEntity user = appUserRepository.findByIdAndDeletedAtIsNull(UUID.fromString(initiator.toString().trim())).orElse(null);
        if (user != null && user.getDepartment() != null) {
          return user.getDepartment().getId();
        }
      } catch (IllegalArgumentException ignored) {
        // not a UUID
      }
    }
    if (correspondence.getOwnerDepartment() != null) {
      return correspondence.getOwnerDepartment().getId();
    }
    return null;
  }

  private Long resolveTargetDepartmentId(
      DelegateExecution execution, CorrespondenceEntity correspondence) {
    Object explicit = execution.getVariable(CorrespondenceWorkflowVariables.TARGET_DEPARTMENT_ID);
    Long id = asLong(explicit);
    if (id != null) {
      return id;
    }
    if (correspondence.getOwnerDepartment() != null) {
      return correspondence.getOwnerDepartment().getId();
    }
    return null;
  }

  private List<Map<String, Object>> toStopMaps(
      List<RoutingStopDto> stops, CorrespondenceEntity correspondence) {
    List<Map<String, Object>> out = new ArrayList<>(stops.size());
    int seq = 0;
    for (RoutingStopDto stop : stops) {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("sequence", seq++);
      m.put("departmentId", stop.departmentId());
      m.put("departmentCode", stop.departmentCode());
      m.put("departmentNameAr", stop.departmentNameAr());
      m.put("departmentNameEn", stop.departmentNameEn());
      m.put("levelCode", stop.levelCode());
      m.put("roleCode", stop.roleCode());
      m.put("reasonKey", stop.reasonKey());
      m.put("slaIso", slaDurationResolver.resolveSlaIso(correspondence, stop.levelCode()));
      out.add(m);
    }
    return out;
  }

  private String writeJson(List<Map<String, Object>> stops) {
    try {
      return objectMapper.writeValueAsString(stops);
    } catch (JsonProcessingException e) {
      log.warn("routingChainDelegate: failed to serialize chain", e);
      return "[]";
    }
  }

  private static Long asLong(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number n) {
      return n.longValue();
    }
    String s = value.toString().trim();
    if (!StringUtils.hasText(s)) {
      return null;
    }
    try {
      return Long.parseLong(s);
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
