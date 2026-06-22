package com.gov.ac.feature.correspondence.workflow;

import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.feature.departments.entity.DepartmentEntity;
import com.gov.ac.feature.departments.repository.DepartmentRepository;
import com.gov.ac.feature.organization.dto.RoutingChainDto;
import com.gov.ac.feature.organization.dto.RoutingStopDto;
import com.gov.ac.feature.lookups.entity.WorkflowActionTypeEntity;
import com.gov.ac.feature.organization.service.OrgRoutingService;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import com.gov.ac.feature.workflow.execution.service.WorkflowService;
import com.gov.ac.common.api.BadRequestException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * REFER reassigns the active routing task without completing the Camunda step. FORWARD updates the
 * correspondence target department and appends routing stops toward that department before the
 * current task is completed normally.
 */
@Service
@RequiredArgsConstructor
public class CorrespondenceReferForwardService {

  private final AppUserRepository appUserRepository;
  private final DepartmentRepository departmentRepository;
  private final CorrespondenceRepository correspondenceRepository;
  private final OrgRoutingService orgRoutingService;
  private final WorkflowService workflowService;
  private final WorkflowSlaDurationResolver slaDurationResolver;
  private final CorrespondenceWorkflowTaskPersistenceService workflowTaskPersistenceService;

  @Transactional
  public void referActiveTask(
      Task task,
      UUID actorUserId,
      UUID targetUserId,
      String comment,
      CorrespondenceEntity correspondence,
      WorkflowActionTypeEntity rule) {
    AppUserEntity target =
        appUserRepository
            .findByIdAndDeletedAtIsNull(targetUserId)
            .orElseThrow(() -> new BadRequestException("Referred user not found"));
    if (!Boolean.TRUE.equals(target.getActive())) {
      throw new BadRequestException("Referred user is not active");
    }

    workflowTaskPersistenceService.recordReferWithoutComplete(
        task, actorUserId, targetUserId, comment, correspondence, rule);
    workflowService.setAssignee(task.getId(), targetUserId.toString());
  }

  @Transactional
  public void prepareForward(
      Task task, Long targetDepartmentId, CorrespondenceEntity correspondence) {
    DepartmentEntity targetDept =
        departmentRepository
            .findByIdAndDeletedAtIsNull(targetDepartmentId)
            .orElseThrow(() -> new BadRequestException("Target department not found"));

    correspondence.setOwnerDepartment(targetDept);
    correspondenceRepository.save(correspondence);

    workflowService.setProcessVariable(
        task.getProcessInstanceId(),
        CorrespondenceWorkflowVariables.TARGET_DEPARTMENT_ID,
        targetDepartmentId);

    appendForwardStops(task, correspondence, targetDepartmentId);
  }

  @SuppressWarnings("unchecked")
  private void appendForwardStops(
      Task task, CorrespondenceEntity correspondence, Long targetDepartmentId) {
    Object stopsRaw =
        workflowService
            .getProcessVariable(task.getProcessInstanceId(), CorrespondenceWorkflowVariables.ROUTING_STOPS)
            .orElse(null);
    List<Map<String, Object>> stops =
        stopsRaw instanceof List<?> list ? new ArrayList<>((List<Map<String, Object>>) list) : new ArrayList<>();

    Long originatorDepartmentId = resolveOriginatorDepartmentId(task, correspondence);
    if (originatorDepartmentId == null) {
      return;
    }

    RoutingChainDto chain = orgRoutingService.computeChain(originatorDepartmentId, targetDepartmentId);
    List<Map<String, Object>> forwardStops = toStopMaps(chain.stops(), correspondence);
    if (forwardStops.isEmpty()) {
      return;
    }

    LinkedHashMap<String, Map<String, Object>> merged = new LinkedHashMap<>();
    for (Map<String, Object> stop : stops) {
      Object deptId = stop.get("departmentId");
      if (deptId != null) {
        merged.put(deptId.toString(), stop);
      }
    }
    for (Map<String, Object> stop : forwardStops) {
      Object deptId = stop.get("departmentId");
      if (deptId != null) {
        merged.putIfAbsent(deptId.toString(), stop);
      }
    }

    workflowService.setProcessVariable(
        task.getProcessInstanceId(),
        CorrespondenceWorkflowVariables.ROUTING_STOPS,
        new ArrayList<>(merged.values()));
  }

  private Long resolveOriginatorDepartmentId(Task task, CorrespondenceEntity correspondence) {
    Object explicit =
        workflowService
            .getProcessVariable(
                task.getProcessInstanceId(), CorrespondenceWorkflowVariables.ORIGINATOR_DEPARTMENT_ID)
            .orElse(null);
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
