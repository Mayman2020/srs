package com.gov.ac.feature.delegation.task.service;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.ForbiddenException;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.feature.audit.dto.CreateAuditEventRequestDto;
import com.gov.ac.feature.audit.service.AuditTrailService;
import com.gov.ac.feature.delegation.entity.AuthorityDelegationEntity;
import com.gov.ac.feature.delegation.repository.AuthorityDelegationRepository;
import com.gov.ac.feature.delegation.task.dto.CreateTaskDelegationRequestDto;
import com.gov.ac.feature.delegation.task.dto.TaskDelegationDto;
import com.gov.ac.feature.delegation.task.dto.TaskDelegationListDto;
import com.gov.ac.feature.delegation.task.entity.TaskDelegationEntity;
import com.gov.ac.feature.delegation.task.mapper.TaskDelegationMapper;
import com.gov.ac.feature.delegation.task.repository.TaskDelegationRepository;
import com.gov.ac.feature.lookups.entity.ConfidentialityEntity;
import com.gov.ac.feature.lookups.repository.ConfidentialityRepository;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for {@link TaskDelegationEntity}. Enforces the three business invariants
 * the spec calls out (Slice 2 §1):
 *
 * <ol>
 *   <li><b>Confidentiality clearance</b> — the delegate's clearance {@code sort_order} must be at
 *       least as restrictive (numerically &le;) as the delegator's. If the new row also names an
 *       explicit {@code allowed_confidentiality_codes} csv, every code must be reachable by the
 *       delegate.
 *   <li><b>Circular chains</b> — a delegation must not form a cycle when chained against any
 *       other currently-active delegations (e.g. {@code A → B} blocked when {@code B → A} exists
 *       on overlapping days).
 *   <li><b>Overlapping duplicate scope</b> — two active rows from the same delegator with the same
 *       scope target on overlapping days are forbidden (concurrent delegations to different
 *       targets are still allowed; the delegate filter resolves at task-assignment time).
 * </ol>
 *
 * <p>All mutating paths emit canonical {@code audit_event} rows so the activity stream can
 * distinguish original assignee, delegator, and acting delegate.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskDelegationService {

  public static final String ACTION_CREATED = "TASK_DELEGATION_CREATED";
  public static final String ACTION_REVOKED = "TASK_DELEGATION_REVOKED";
  public static final String ACTION_EXPIRED = "TASK_DELEGATION_EXPIRED";
  public static final String ACTION_USED = "TASK_ACTED_UNDER_DELEGATION";
  public static final String RESOURCE_TYPE = "TASK_DELEGATION";

  private final TaskDelegationRepository taskDelegationRepository;
  private final AuthorityDelegationRepository authorityDelegationRepository;
  private final AppUserRepository appUserRepository;
  private final ConfidentialityRepository confidentialityRepository;
  private final AuditTrailService auditTrailService;

  @Transactional(readOnly = true)
  public TaskDelegationListDto listForUser(UUID userId) {
    LocalDate today = LocalDate.now();
    List<TaskDelegationEntity> all = taskDelegationRepository.findVisibleForUser(userId);
    List<TaskDelegationDto> outgoingActive = new ArrayList<>();
    List<TaskDelegationDto> incomingActive = new ArrayList<>();
    List<TaskDelegationDto> inactive = new ArrayList<>();
    for (TaskDelegationEntity row : all) {
      TaskDelegationDto dto = TaskDelegationMapper.toDto(row, today);
      if (dto.active()) {
        if (row.getDelegatorUser().getId().equals(userId)) {
          outgoingActive.add(dto);
        } else {
          incomingActive.add(dto);
        }
      } else {
        inactive.add(dto);
      }
    }
    return new TaskDelegationListDto(outgoingActive, incomingActive, inactive);
  }

  @Transactional
  public TaskDelegationDto create(UUID currentUserId, CreateTaskDelegationRequestDto req) {
    LocalDate today = LocalDate.now();

    if (req.delegateUserId().equals(currentUserId)) {
      throw new BadRequestException("Cannot delegate to yourself");
    }
    if (req.validTo().isBefore(req.validFrom())) {
      throw new BadRequestException("validTo must be on or after validFrom");
    }
    if (req.validTo().isBefore(today)) {
      throw new BadRequestException("Delegation must end on or after today");
    }
    String scope = normaliseScope(req.scopeType());
    if (TaskDelegationEntity.SCOPE_TASK.equals(scope)
        && req.camundaTaskId() == null
        && req.correspondenceId() == null) {
      throw new BadRequestException(
          "scopeType=TASK requires either camundaTaskId or correspondenceId");
    }

    AppUserEntity delegator =
        appUserRepository
            .findByIdAndDeletedAtIsNull(currentUserId)
            .orElseThrow(() -> new NotFoundException("Delegator user not found"));
    if (!Boolean.TRUE.equals(delegator.getActive())) {
      throw new BadRequestException("Inactive user cannot create delegations");
    }
    AppUserEntity delegate =
        appUserRepository
            .findByIdAndDeletedAtIsNull(req.delegateUserId())
            .orElseThrow(() -> new BadRequestException("Unknown delegate user"));
    if (!Boolean.TRUE.equals(delegate.getActive())) {
      throw new BadRequestException("Inactive delegate user");
    }

    assertClearanceBoundary(delegator, delegate, req.allowedConfidentialityCodes());
    assertNoCycle(currentUserId, req.delegateUserId(), req.validFrom(), req.validTo());
    assertNoOverlappingDuplicate(currentUserId, scope, req);

    AuthorityDelegationEntity linkedAuthority = null;
    if (req.authorityDelegationId() != null) {
      linkedAuthority =
          authorityDelegationRepository
              .findByIdAndDeletedAtIsNull(req.authorityDelegationId())
              .orElseThrow(
                  () ->
                      new BadRequestException(
                          "Linked authority delegation not found or already revoked"));
      if (!linkedAuthority.getDelegatorUser().getId().equals(currentUserId)) {
        throw new ForbiddenException(
            "You may only link to authority delegations where you are the delegator");
      }
    }

    TaskDelegationEntity entity = new TaskDelegationEntity();
    entity.setDelegatorUser(delegator);
    entity.setDelegateUser(delegate);
    entity.setScopeType(scope);
    entity.setCorrespondenceId(req.correspondenceId());
    entity.setCamundaTaskId(trimToNull(req.camundaTaskId()));
    entity.setProcessInstanceId(trimToNull(req.processInstanceId()));
    entity.setAllowedCorrespondenceTypeCodes(trimToNull(req.allowedCorrespondenceTypeCodes()));
    entity.setAllowedConfidentialityCodes(trimToNull(req.allowedConfidentialityCodes()));
    entity.setValidFrom(req.validFrom());
    entity.setValidTo(req.validTo());
    entity.setNotes(trimToNull(req.notes()));
    entity.setAuthorityDelegation(linkedAuthority);
    entity.setCreatedBy(currentUserId);
    entity.setUpdatedBy(currentUserId);

    TaskDelegationEntity saved = taskDelegationRepository.save(entity);
    emitAuditEvent(currentUserId, ACTION_CREATED, saved);
    log.info(
        "Task delegation created id={} delegator={} delegate={} scope={} validFrom={} validTo={}",
        saved.getId(),
        delegator.getId(),
        delegate.getId(),
        scope,
        saved.getValidFrom(),
        saved.getValidTo());
    return TaskDelegationMapper.toDto(saved, today);
  }

  @Transactional
  public void revoke(UUID currentUserId, UUID delegationId, boolean mayRevokeAsAdmin) {
    TaskDelegationEntity row =
        taskDelegationRepository
            .findByIdAndRevokedAtIsNull(delegationId)
            .orElseThrow(
                () -> new NotFoundException("Task delegation not found or already revoked"));
    boolean isDelegator = row.getDelegatorUser().getId().equals(currentUserId);
    if (!isDelegator && !mayRevokeAsAdmin) {
      throw new ForbiddenException(
          "Only the delegator (or an administrator) can revoke this delegation");
    }
    row.setRevokedAt(Instant.now());
    row.setRevokedBy(currentUserId);
    row.setUpdatedBy(currentUserId);
    taskDelegationRepository.save(row);
    emitAuditEvent(currentUserId, ACTION_REVOKED, row);
    log.info(
        "Task delegation revoked id={} by user={} (delegator={}, delegate={})",
        row.getId(),
        currentUserId,
        row.getDelegatorUser().getId(),
        row.getDelegateUser().getId());
  }

  /**
   * Idempotent batch: marks every non-revoked row whose {@code valid_to} is strictly before
   * today as revoked by the system. Running again no-ops because the predicate skips revoked
   * rows.
   */
  @Transactional
  public int expireOverdue(LocalDate today) {
    List<TaskDelegationEntity> overdue = taskDelegationRepository.findExpiredAsOf(today);
    if (overdue.isEmpty()) {
      return 0;
    }
    Instant now = Instant.now();
    int count = 0;
    for (TaskDelegationEntity row : overdue) {
      // Double-check inside the loop in case a concurrent revoke landed between query and update.
      if (row.getRevokedAt() != null) {
        continue;
      }
      row.setRevokedAt(now);
      row.setRevokedBy(null); // null actor = system expiry
      row.setUpdatedBy(null);
      taskDelegationRepository.save(row);
      emitAuditEvent(null, ACTION_EXPIRED, row);
      count++;
    }
    if (count > 0) {
      log.info("Task delegation expiry job revoked {} row(s) on {}", count, today);
    }
    return count;
  }

  // ---------------------------------------------------------------------------
  // Lookup APIs used by the Camunda assignment listener / inbox query.
  // These are read-only and intentionally tolerate missing/invalid inputs by
  // returning Optional.empty() so the caller can fall back to the original
  // assignee without throwing.
  // ---------------------------------------------------------------------------

  @Transactional(readOnly = true)
  public Optional<TaskDelegationEntity> findEffectiveDelegationForTask(
      UUID delegatorUserId,
      String camundaTaskId,
      UUID correspondenceId,
      String correspondenceTypeCode,
      String confidentialityCode) {
    if (delegatorUserId == null) {
      return Optional.empty();
    }
    LocalDate today = LocalDate.now();

    // 1) Task-scoped wins (most specific).
    if (camundaTaskId != null || correspondenceId != null) {
      List<TaskDelegationEntity> specific =
          taskDelegationRepository.findActiveTaskScoped(
              delegatorUserId, camundaTaskId, correspondenceId, today);
      Optional<TaskDelegationEntity> match =
          specific.stream()
              .filter(d -> matchesFilters(d, correspondenceTypeCode, confidentialityCode))
              .findFirst();
      if (match.isPresent()) {
        return match;
      }
    }

    // 2) Broad scope: TYPE_CONFIDENTIALITY matching the correspondence attributes.
    List<TaskDelegationEntity> outgoing =
        taskDelegationRepository.findActiveByDelegator(delegatorUserId, today);
    return outgoing.stream()
        .filter(d -> TaskDelegationEntity.SCOPE_TYPE_CONFIDENTIALITY.equals(d.getScopeType()))
        .filter(d -> matchesFilters(d, correspondenceTypeCode, confidentialityCode))
        .findFirst();
  }

  /**
   * Records an audit row whenever the Camunda assignment listener actually rewired a task to a
   * delegate. Separate action code so audit consumers can count "delegations that fired" without
   * trawling Camunda history.
   */
  @Transactional
  public void recordTaskRoutedToDelegate(
      TaskDelegationEntity delegation, String camundaTaskId, UUID correspondenceId) {
    if (delegation == null) {
      return;
    }
    auditTrailService.append(
        new CreateAuditEventRequestDto(
            delegation.getDelegateUser().getId().toString(),
            ACTION_USED,
            RESOURCE_TYPE,
            delegation.getId().toString(),
            buildDetailJson(
                delegation,
                "{\"camundaTaskId\":\""
                    + safe(camundaTaskId)
                    + "\",\"correspondenceId\":\""
                    + (correspondenceId != null ? correspondenceId.toString() : "")
                    + "\"}"),
            null,
            null,
            Instant.now()));
  }

  // ---------------------------------------------------------------------------
  // Invariants
  // ---------------------------------------------------------------------------

  private void assertClearanceBoundary(
      AppUserEntity delegator, AppUserEntity delegate, String allowedConfidentialityCodesCsv) {
    Integer delegatorOrder = clearanceSortOrder(delegator.getSecurityClearanceId());
    Integer delegateOrder = clearanceSortOrder(delegate.getSecurityClearanceId());

    // Lower sort_order = more restrictive level (matches CorrespondenceViewAuthorization).
    // The delegate must be at least as cleared as the delegator, otherwise we could route
    // restricted material to someone who cannot view it.
    int dgOrder = delegatorOrder == null ? Integer.MAX_VALUE : delegatorOrder;
    int deOrder = delegateOrder == null ? Integer.MAX_VALUE : delegateOrder;
    if (deOrder > dgOrder) {
      throw new ForbiddenException(
          "Delegate clearance is lower than yours; delegation would bypass confidentiality.");
    }

    if (allowedConfidentialityCodesCsv == null || allowedConfidentialityCodesCsv.isBlank()) {
      return;
    }
    for (String code : allowedConfidentialityCodesCsv.split(",")) {
      String trimmed = code.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      ConfidentialityEntity level =
          confidentialityRepository
              .findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(trimmed)
              .orElse(null);
      if (level == null) {
        throw new BadRequestException("Unknown confidentiality code: " + trimmed);
      }
      if (Boolean.TRUE.equals(level.getRequiresClearance())) {
        int required = level.getSortOrder() == null ? Integer.MAX_VALUE : level.getSortOrder();
        if (deOrder > required) {
          throw new ForbiddenException(
              "Delegate is not cleared for confidentiality '" + level.getCode() + "'");
        }
      }
    }
  }

  private Integer clearanceSortOrder(Long clearanceId) {
    if (clearanceId == null) {
      return null;
    }
    return confidentialityRepository
        .findByIdAndDeletedAtIsNull(clearanceId)
        .map(ConfidentialityEntity::getSortOrder)
        .orElse(null);
  }

  private void assertNoCycle(
      UUID delegatorId, UUID delegateId, LocalDate validFrom, LocalDate validTo) {
    // BFS along current active delegate→delegate edges; if we can walk from the proposed
    // delegate back to the delegator on overlapping days, the new edge would close a cycle.
    Set<UUID> visited = new HashSet<>();
    List<UUID> frontier = new ArrayList<>();
    frontier.add(delegateId);
    while (!frontier.isEmpty()) {
      List<UUID> next = new ArrayList<>();
      for (UUID node : frontier) {
        if (!visited.add(node)) {
          continue;
        }
        // Use validFrom as the probe day: any chain that "exists somewhere in the new window"
        // is enough to close a cycle. We scan all active rows where this node is the delegator.
        List<TaskDelegationEntity> edges = new ArrayList<>();
        edges.addAll(taskDelegationRepository.findActiveByDelegator(node, validFrom));
        // Also probe validTo to catch cycles that only materialise late in the window.
        edges.addAll(taskDelegationRepository.findActiveByDelegator(node, validTo));
        for (TaskDelegationEntity edge : edges) {
          if (!datesOverlap(edge.getValidFrom(), edge.getValidTo(), validFrom, validTo)) {
            continue;
          }
          UUID downstream = edge.getDelegateUser().getId();
          if (downstream.equals(delegatorId)) {
            throw new BadRequestException(
                "Delegation would create a circular delegation chain.");
          }
          next.add(downstream);
        }
      }
      frontier = next;
    }
  }

  private void assertNoOverlappingDuplicate(
      UUID delegatorId, String scope, CreateTaskDelegationRequestDto req) {
    List<TaskDelegationEntity> overlapping =
        taskDelegationRepository.findOverlappingByDelegator(
            delegatorId, req.validFrom(), req.validTo());
    for (TaskDelegationEntity row : overlapping) {
      if (!sameScope(row, scope, req)) {
        continue;
      }
      // Same scope (same task / same correspondence / same csv filters) overlapping in time —
      // reject. A different delegate on the same scope is still a duplicate.
      throw new BadRequestException(
          "An active delegation already covers this scope for the requested window.");
    }
  }

  private static boolean sameScope(
      TaskDelegationEntity row, String scope, CreateTaskDelegationRequestDto req) {
    if (!scope.equals(row.getScopeType())) {
      return false;
    }
    if (TaskDelegationEntity.SCOPE_TASK.equals(scope)) {
      return equal(row.getCamundaTaskId(), trimToNull(req.camundaTaskId()))
          && equal(row.getCorrespondenceId(), req.correspondenceId());
    }
    // TYPE_CONFIDENTIALITY duplicates only when the csv filters match exactly.
    return equal(
            normaliseCsv(row.getAllowedCorrespondenceTypeCodes()),
            normaliseCsv(req.allowedCorrespondenceTypeCodes()))
        && equal(
            normaliseCsv(row.getAllowedConfidentialityCodes()),
            normaliseCsv(req.allowedConfidentialityCodes()));
  }

  private static boolean datesOverlap(LocalDate aFrom, LocalDate aTo, LocalDate bFrom, LocalDate bTo) {
    return !aFrom.isAfter(bTo) && !aTo.isBefore(bFrom);
  }

  private static boolean matchesFilters(
      TaskDelegationEntity delegation,
      String correspondenceTypeCode,
      String confidentialityCode) {
    if (!csvContains(delegation.getAllowedCorrespondenceTypeCodes(), correspondenceTypeCode)) {
      return false;
    }
    return csvContains(delegation.getAllowedConfidentialityCodes(), confidentialityCode);
  }

  private static boolean csvContains(String csv, String code) {
    if (csv == null || csv.isBlank()) {
      // empty filter = allow all
      return true;
    }
    if (code == null) {
      // delegate restricted the filter but the correspondence has no value — deny.
      return false;
    }
    for (String entry : csv.split(",")) {
      if (entry.trim().equalsIgnoreCase(code.trim())) {
        return true;
      }
    }
    return false;
  }

  private void emitAuditEvent(UUID actorUserId, String actionCode, TaskDelegationEntity row) {
    String actor = actorUserId != null ? actorUserId.toString() : "SYSTEM";
    auditTrailService.append(
        new CreateAuditEventRequestDto(
            actor,
            actionCode,
            RESOURCE_TYPE,
            row.getId().toString(),
            buildDetailJson(row, null),
            null,
            null,
            Instant.now()));
  }

  private static String buildDetailJson(TaskDelegationEntity row, String extra) {
    StringBuilder sb = new StringBuilder();
    sb.append("{\"delegatorId\":\"")
        .append(row.getDelegatorUser().getId())
        .append("\",\"delegateId\":\"")
        .append(row.getDelegateUser().getId())
        .append("\",\"scopeType\":\"")
        .append(safe(row.getScopeType()))
        .append("\",\"validFrom\":\"")
        .append(row.getValidFrom())
        .append("\",\"validTo\":\"")
        .append(row.getValidTo())
        .append("\"");
    if (row.getCamundaTaskId() != null) {
      sb.append(",\"camundaTaskId\":\"").append(safe(row.getCamundaTaskId())).append("\"");
    }
    if (row.getCorrespondenceId() != null) {
      sb.append(",\"correspondenceId\":\"").append(row.getCorrespondenceId()).append("\"");
    }
    if (extra != null && !extra.isBlank()) {
      sb.append(",\"extra\":").append(extra);
    }
    sb.append("}");
    return sb.toString();
  }

  private static String safe(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private static String normaliseScope(String raw) {
    if (raw == null) {
      throw new BadRequestException("scopeType is required");
    }
    String upper = raw.trim().toUpperCase();
    if (!TaskDelegationEntity.SCOPE_TASK.equals(upper)
        && !TaskDelegationEntity.SCOPE_TYPE_CONFIDENTIALITY.equals(upper)) {
      throw new BadRequestException("Unsupported scopeType: " + raw);
    }
    return upper;
  }

  private static String trimToNull(String raw) {
    if (raw == null) {
      return null;
    }
    String trimmed = raw.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static String normaliseCsv(String csv) {
    if (csv == null || csv.isBlank()) {
      return null;
    }
    return java.util.Arrays.stream(csv.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(String::toUpperCase)
        .distinct()
        .sorted()
        .collect(Collectors.joining(","));
  }

  private static boolean equal(Object a, Object b) {
    return a == null ? b == null : a.equals(b);
  }
}
