package com.gov.ac.feature.sla.service;

import com.gov.ac.feature.audit.dto.CreateAuditEventRequestDto;
import com.gov.ac.feature.audit.service.AuditTrailService;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.delegation.entity.AuthorityDelegationEntity;
import com.gov.ac.feature.delegation.repository.AuthorityDelegationRepository;
import com.gov.ac.feature.departments.entity.DepartmentEntity;
import com.gov.ac.feature.departments.repository.DepartmentRepository;
import com.gov.ac.feature.organization.entity.OrganizationalUnitLevelEntity;
import com.gov.ac.feature.organization.repository.OrganizationalUnitLevelRepository;
import com.gov.ac.feature.organization.service.OrgLevelRoleResolver;
import com.gov.ac.feature.sla.entity.SlaBreachEventEntity;
import com.gov.ac.feature.sla.entity.SlaEscalationStepEntity;
import com.gov.ac.feature.sla.metrics.SlaMetrics;
import com.gov.ac.feature.sla.notification.SlaNotifier;
import com.gov.ac.feature.sla.repository.SlaBreachEventRepository;
import com.gov.ac.feature.users.repository.UserRoleRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.TaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Executes the escalation actions described by an {@link SlaEscalationStepEntity}. Every action is
 * funnelled through {@link SlaClearanceFilter} so escalation can never widen the audience past the
 * correspondence's confidentiality. Successes increment {@code correspondence_sla_escalation_total{
 * action,process}} and emit a canonical audit row; failures log a warning and return without
 * advancing the breach event so the evaluation job retries on the next tick.
 *
 * <p>Idempotency contract: the calling job advances {@code last_step_executed_order} only after
 * this service returns {@code true}, so a transient failure (e.g. notification table briefly
 * unavailable) does not mark the step as done.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SlaEscalationService {

  public static final String ROLE_DEPT_MANAGER = "DEPT_MANAGER";
  public static final String ROLE_HQ_OFFICER = "HQ_OFFICER";
  public static final String ROLE_BRIGADE_OFFICER = "BRIGADE_OFFICER";
  public static final String ROLE_STAFF = "STAFF";
  public static final String ROLE_SYS_ADMIN = "SYS_ADMIN";
  public static final String ROLE_AUDITOR = "AUDITOR";

  public static final String AUDIT_BREACH_DETECTED = "SLA_BREACH_DETECTED";
  public static final String AUDIT_STEP_EXECUTED = "SLA_ESCALATION_STEP_EXECUTED";
  public static final String AUDIT_BREACH_RESOLVED = "SLA_BREACH_RESOLVED";
  public static final String RESOURCE_TYPE = "SLA_BREACH_EVENT";

  private final TaskService taskService;
  private final AuthorityDelegationRepository authorityDelegationRepository;
  private final DepartmentRepository departmentRepository;
  private final OrganizationalUnitLevelRepository organizationalUnitLevelRepository;
  private final OrgLevelRoleResolver orgLevelRoleResolver;
  private final UserRoleRepository userRoleRepository;
  private final SlaBreachEventRepository slaBreachEventRepository;
  private final SlaClearanceFilter slaClearanceFilter;
  private final SlaNotifier slaNotifier;
  private final SlaMetrics slaMetrics;
  private final AuditTrailService auditTrailService;

  /**
   * Runs a single escalation step. Returns true if the step actually completed (or was a clean
   * no-op with no target available); false on a hard failure that should be retried later.
   */
  @Transactional
  public boolean executeStep(
      SlaBreachEventEntity breach,
      SlaEscalationStepEntity step,
      CorrespondenceEntity correspondence,
      String currentAssigneeId,
      String processKey) {
    String action = step.getActionCode();
    boolean ok;
    try {
      ok = switch (action) {
        case SlaEscalationStepEntity.ACTION_NOTIFY_MANAGER ->
            notifyManager(breach, correspondence, currentAssigneeId, processKey);
        case SlaEscalationStepEntity.ACTION_REASSIGN_TO_DELEGATE ->
            reassignToDelegate(breach, correspondence, currentAssigneeId, processKey);
        case SlaEscalationStepEntity.ACTION_ESCALATE_TO_HIGHER_LEVEL ->
            escalateToHigherLevel(breach, correspondence, processKey);
        case SlaEscalationStepEntity.ACTION_NOTIFY_AUDIT_ADMIN ->
            notifyAuditAdmin(breach, correspondence, processKey);
        default -> {
          log.warn("[SLA] Unsupported escalation action code: {}", action);
          yield false;
        }
      };
    } catch (RuntimeException ex) {
      log.warn(
          "[SLA] escalation step failed taskId={} action={}: {}",
          breach.getTaskId(),
          action,
          ex.getMessage());
      return false;
    }
    if (ok) {
      slaMetrics.recordEscalationStep(action, processKey);
      auditStepExecuted(breach, step);
    }
    return ok;
  }

  /** Stamps {@code resolved_at} when the underlying Camunda task completes. Idempotent. */
  @Transactional
  public void markResolved(String taskId, String outcome, String processKey) {
    if (taskId == null) {
      return;
    }
    Optional<SlaBreachEventEntity> opt = slaBreachEventRepository.findByTaskId(taskId);
    if (opt.isEmpty()) {
      return;
    }
    SlaBreachEventEntity breach = opt.get();
    if (breach.getResolvedAt() != null) {
      return;
    }
    breach.setResolvedAt(Instant.now());
    breach.setResolutionOutcome(outcome);
    slaBreachEventRepository.save(breach);
    slaMetrics.recordBreachOutcome(SlaMetrics.OUTCOME_BREACH_RESOLVED, processKey);
    slaMetrics.refreshOverdueGauge();
    auditTrailService.append(
        new CreateAuditEventRequestDto(
            "SYSTEM",
            AUDIT_BREACH_RESOLVED,
            RESOURCE_TYPE,
            String.valueOf(breach.getId()),
            buildDetailJson(breach, Map.of("outcome", outcome == null ? "" : outcome)),
            null,
            null,
            Instant.now()));
  }

  /** Emits the canonical {@code SLA_BREACH_DETECTED} audit row exactly once per breach. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void auditBreachDetected(SlaBreachEventEntity breach) {
    auditTrailService.append(
        new CreateAuditEventRequestDto(
            "SYSTEM",
            AUDIT_BREACH_DETECTED,
            RESOURCE_TYPE,
            String.valueOf(breach.getId()),
            buildDetailJson(breach, Map.of()),
            null,
            null,
            Instant.now()));
  }

  // ---------------------------------------------------------------------------
  // Action implementations
  // ---------------------------------------------------------------------------

  private boolean notifyManager(
      SlaBreachEventEntity breach,
      CorrespondenceEntity correspondence,
      String currentAssigneeId,
      String processKey) {
    UUID assigneeUuid = parseUuid(currentAssigneeId).orElse(null);
    if (assigneeUuid == null) {
      log.debug(
          "[SLA] notifyManager skipped: no parseable assignee for taskId={}", breach.getTaskId());
      return true; // nothing to do, but not a failure
    }
    Long deptId = breach.getCorrespondence() != null && breach.getCorrespondence().getOwnerDepartment() != null
        ? breach.getCorrespondence().getOwnerDepartment().getId()
        : null;
    Set<UUID> candidates = new LinkedHashSet<>();
    if (deptId != null) {
      candidates.addAll(
          userRoleRepository.findActiveUserIdsByRoleCodeAndDepartmentId(ROLE_DEPT_MANAGER, deptId));
    }
    // Also walk up to the parent department so an S-level task reaches the K-level manager.
    DepartmentEntity dept =
        deptId != null ? departmentRepository.findByIdAndDeletedAtIsNull(deptId).orElse(null) : null;
    if (dept != null && dept.getParent() != null) {
      candidates.addAll(
          userRoleRepository.findActiveUserIdsByRoleCodeAndDepartmentId(
              ROLE_DEPT_MANAGER, dept.getParent().getId()));
    }
    candidates.remove(assigneeUuid); // don't notify the offender themselves
    List<UUID> cleared = slaClearanceFilter.filter(correspondence, candidates);
    if (cleared.isEmpty()) {
      log.debug(
          "[SLA] notifyManager produced 0 cleared candidates taskId={} deptId={}",
          breach.getTaskId(),
          deptId);
      return true;
    }
    Map<String, Object> params = baseParams(breach, correspondence);
    params.put("action", SlaEscalationStepEntity.ACTION_NOTIFY_MANAGER);
    slaNotifier.notifyRecipients(cleared, SlaNotifier.DEFAULT_EVENT_CODE, correspondence, SlaNotifier.MESSAGE_KEY_SLA_BREACH, params);
    return true;
  }

  private boolean reassignToDelegate(
      SlaBreachEventEntity breach,
      CorrespondenceEntity correspondence,
      String currentAssigneeId,
      String processKey) {
    UUID assigneeUuid = parseUuid(currentAssigneeId).orElse(null);
    if (assigneeUuid == null) {
      return true;
    }
    AuthorityDelegationEntity delegation =
        authorityDelegationRepository
            .findFirstActiveByDelegator(assigneeUuid, Instant.now())
            .orElse(null);
    if (delegation == null || delegation.getDelegateUser() == null) {
      return true;
    }
    UUID delegateId = delegation.getDelegateUser().getId();
    // Hard clearance gate: never reassign to a less-cleared user, even when an authority
    // delegation exists. Authority delegation predates Slice 2 and does not itself enforce
    // confidentiality; the SLA engine is the right place to add that boundary check.
    if (!slaClearanceFilter.isCleared(correspondence, delegateId)) {
      log.warn(
          "[SLA] reassignToDelegate refused: delegate {} not cleared for correspondence {}",
          delegateId,
          breach.correspondenceId());
      return true;
    }
    try {
      taskService.setAssignee(breach.getTaskId(), delegateId.toString());
    } catch (RuntimeException ex) {
      log.warn(
          "[SLA] reassignToDelegate failed taskId={} : {}", breach.getTaskId(), ex.getMessage());
      return false;
    }
    return true;
  }

  private boolean escalateToHigherLevel(
      SlaBreachEventEntity breach, CorrespondenceEntity correspondence, String processKey) {
    Long deptId =
        breach.getWorkflowInstance() != null
            ? breach.getWorkflowInstance().getCurrentDepartmentId()
            : (correspondence != null && correspondence.getOwnerDepartment() != null
                ? correspondence.getOwnerDepartment().getId()
                : null);
    if (deptId == null) {
      return true;
    }
    DepartmentEntity current =
        departmentRepository.findByIdAndDeletedAtIsNull(deptId).orElse(null);
    if (current == null) {
      return true;
    }
    DepartmentEntity higher = resolveHigherLevel(current);
    if (higher == null) {
      log.debug("[SLA] escalateToHigherLevel: no higher department resolvable from {}", deptId);
      return true;
    }
    String roleForLevel = orgLevelRoleResolver.resolveRoleCode(higher.getLevelCode());
    Set<UUID> candidates =
        new LinkedHashSet<>(
            userRoleRepository.findActiveUserIdsByRoleCodeAndDepartmentId(
                roleForLevel, higher.getId()));
    List<UUID> cleared = slaClearanceFilter.filter(correspondence, candidates);
    if (cleared.isEmpty()) {
      return true;
    }
    Map<String, Object> params = baseParams(breach, correspondence);
    params.put("action", SlaEscalationStepEntity.ACTION_ESCALATE_TO_HIGHER_LEVEL);
    params.put("higherLevelCode", safe(higher.getLevelCode()));
    params.put("higherDepartmentId", String.valueOf(higher.getId()));
    slaNotifier.notifyRecipients(cleared, SlaNotifier.DEFAULT_EVENT_CODE, correspondence, SlaNotifier.MESSAGE_KEY_SLA_BREACH, params);
    return true;
  }

  private boolean notifyAuditAdmin(
      SlaBreachEventEntity breach, CorrespondenceEntity correspondence, String processKey) {
    List<UUID> candidates =
        userRoleRepository.findActiveUserIdsByRoleCodes(List.of(ROLE_SYS_ADMIN, ROLE_AUDITOR));
    List<UUID> cleared = slaClearanceFilter.filter(correspondence, candidates);
    if (cleared.isEmpty()) {
      return true;
    }
    Map<String, Object> params = baseParams(breach, correspondence);
    params.put("action", SlaEscalationStepEntity.ACTION_NOTIFY_AUDIT_ADMIN);
    slaNotifier.notifyRecipients(cleared, SlaNotifier.DEFAULT_EVENT_CODE, correspondence, SlaNotifier.MESSAGE_KEY_SLA_BREACH, params);
    return true;
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  /**
   * Walks the department parent chain looking for the next ancestor whose level outranks the
   * current node ({@code rank_order} strictly less than current). Returns null if the current
   * node is already at Q or the chain is unresolvable.
   */
  private DepartmentEntity resolveHigherLevel(DepartmentEntity current) {
    Integer currentRank = rankFor(current.getLevelCode());
    if (currentRank == null) {
      return current.getParent();
    }
    DepartmentEntity cursor = current.getParent();
    int safety = 16;
    while (cursor != null && safety-- > 0) {
      Integer cursorRank = rankFor(cursor.getLevelCode());
      if (cursorRank != null && cursorRank < currentRank) {
        return cursor;
      }
      cursor = cursor.getParent();
    }
    return null;
  }

  private Integer rankFor(String levelCode) {
    if (levelCode == null || levelCode.isBlank()) {
      return null;
    }
    OrganizationalUnitLevelEntity level =
        organizationalUnitLevelRepository.findActiveByCode(levelCode).orElse(null);
    return level == null ? null : level.getRankOrder();
  }

  private void auditStepExecuted(SlaBreachEventEntity breach, SlaEscalationStepEntity step) {
    Map<String, String> extra = new HashMap<>();
    extra.put("stepOrder", String.valueOf(step.getStepOrder()));
    extra.put("actionCode", step.getActionCode());
    if (step.getPolicy() != null && step.getPolicy().getId() != null) {
      extra.put("slaPolicyId", String.valueOf(step.getPolicy().getId()));
    }
    auditTrailService.append(
        new CreateAuditEventRequestDto(
            "SYSTEM",
            AUDIT_STEP_EXECUTED,
            RESOURCE_TYPE,
            String.valueOf(breach.getId()),
            buildDetailJson(breach, extra),
            null,
            null,
            Instant.now()));
  }

  private static Map<String, Object> baseParams(
      SlaBreachEventEntity breach, CorrespondenceEntity correspondence) {
    Map<String, Object> params = new HashMap<>();
    params.put("taskId", breach.getTaskId());
    if (breach.getTargetAt() != null) {
      params.put("targetAt", breach.getTargetAt().toString());
    }
    if (correspondence != null) {
      params.put("correspondenceId", correspondence.getId().toString());
      params.put("referenceNumber", safe(correspondence.getReferenceNumber()));
      params.put("subject", safe(correspondence.getSubject()));
    }
    return params;
  }

  private static String buildDetailJson(SlaBreachEventEntity breach, Map<String, ?> extra) {
    StringBuilder sb = new StringBuilder();
    sb.append("{\"taskId\":\"").append(safe(breach.getTaskId())).append("\"");
    if (breach.getProcessInstanceId() != null) {
      sb.append(",\"processInstanceId\":\"")
          .append(safe(breach.getProcessInstanceId()))
          .append("\"");
    }
    if (breach.correspondenceId() != null) {
      sb.append(",\"correspondenceId\":\"").append(breach.correspondenceId()).append("\"");
    }
    if (breach.getPolicy() != null && breach.getPolicy().getId() != null) {
      sb.append(",\"slaPolicyId\":").append(breach.getPolicy().getId());
    }
    if (breach.getTargetAt() != null) {
      sb.append(",\"targetAt\":\"").append(breach.getTargetAt()).append("\"");
    }
    if (breach.getBreachedAt() != null) {
      sb.append(",\"breachedAt\":\"").append(breach.getBreachedAt()).append("\"");
    }
    if (extra != null) {
      for (Map.Entry<String, ?> e : extra.entrySet()) {
        sb.append(",\"")
            .append(safe(e.getKey()))
            .append("\":\"")
            .append(safe(String.valueOf(e.getValue())))
            .append("\"");
      }
    }
    sb.append("}");
    return sb.toString();
  }

  private static Optional<UUID> parseUuid(String value) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(UUID.fromString(value.trim()));
    } catch (IllegalArgumentException ex) {
      return Optional.empty();
    }
  }

  private static String safe(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
