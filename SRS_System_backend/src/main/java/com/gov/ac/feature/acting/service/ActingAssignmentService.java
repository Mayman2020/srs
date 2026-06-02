package com.gov.ac.feature.acting.service;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.ForbiddenException;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.feature.audit.dto.CreateAuditEventRequestDto;
import com.gov.ac.feature.audit.service.AuditTrailService;
import com.gov.ac.feature.acting.dto.ActingAssignmentDto;
import com.gov.ac.feature.acting.dto.ActingAssignmentListDto;
import com.gov.ac.feature.acting.dto.CreateActingAssignmentRequestDto;
import com.gov.ac.feature.acting.entity.ActingAssignmentEntity;
import com.gov.ac.feature.acting.repository.ActingAssignmentRepository;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.departments.entity.DepartmentEntity;
import com.gov.ac.feature.departments.repository.DepartmentRepository;
import com.gov.ac.feature.lookups.entity.ConfidentialityEntity;
import com.gov.ac.feature.lookups.entity.CorrespondenceTypeEntity;
import com.gov.ac.feature.lookups.entity.WorkflowActionTypeEntity;
import com.gov.ac.feature.lookups.repository.ConfidentialityRepository;
import com.gov.ac.feature.lookups.repository.CorrespondenceTypeRepository;
import com.gov.ac.feature.lookups.repository.WorkflowActionTypeRepository;
import com.gov.ac.feature.sla.service.SlaClearanceFilter;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActingAssignmentService {

  public static final String ACTION_CREATED = "ACTING_ASSIGNMENT_CREATED";
  public static final String ACTION_REVOKED = "ACTING_ASSIGNMENT_REVOKED";
  public static final String ACTION_EXPIRED = "ACTING_ASSIGNMENT_EXPIRED";
  public static final String ACTION_USED = "ACTING_ASSIGNMENT_USED";
  public static final String RESOURCE_TYPE = "ACTING_ASSIGNMENT";

  private final ActingAssignmentRepository actingAssignmentRepository;
  private final AppUserRepository appUserRepository;
  private final DepartmentRepository departmentRepository;
  private final CorrespondenceTypeRepository correspondenceTypeRepository;
  private final ConfidentialityRepository confidentialityRepository;
  private final WorkflowActionTypeRepository workflowActionTypeRepository;
  private final AuditTrailService auditTrailService;
  private final ActingAssignmentMatcher actingAssignmentMatcher;
  private final SlaClearanceFilter slaClearanceFilter;

  @Transactional(readOnly = true)
  public ActingAssignmentListDto listForUser(UUID userId) {
    LocalDate today = LocalDate.now();
    List<ActingAssignmentEntity> asAbsent = actingAssignmentRepository.findActiveByAbsentUser(userId, today);
    List<ActingAssignmentEntity> asActing = actingAssignmentRepository.findActiveByActingUser(userId, today);
    List<ActingAssignmentEntity> upcoming =
        actingAssignmentRepository.findUpcomingForUser(userId, today).stream().limit(50).toList();
    List<ActingAssignmentEntity> inactive =
        actingAssignmentRepository.findInactiveForUser(userId, today).stream().limit(50).toList();
    return new ActingAssignmentListDto(
        asAbsent.stream().map(r -> toDto(r, today)).toList(),
        asActing.stream().map(r -> toDto(r, today)).toList(),
        upcoming.stream().map(r -> toDto(r, today)).toList(),
        inactive.stream().map(r -> toDto(r, today)).toList());
  }

  @Transactional(readOnly = true)
  public List<ActingAssignmentDto> listAllForAudit() {
    LocalDate today = LocalDate.now();
    return actingAssignmentRepository.findAll().stream()
        .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
        .limit(500)
        .map(a -> toDto(a, today))
        .toList();
  }

  @Transactional
  public ActingAssignmentDto create(UUID actorId, CreateActingAssignmentRequestDto req, boolean asAdmin) {
    LocalDate today = LocalDate.now();
    if (req.validTo().isBefore(req.validFrom())) {
      throw new BadRequestException("validTo must be on or after validFrom");
    }
    if (req.actingUserId().equals(req.absentUserId())) {
      throw new BadRequestException("Absent user and acting user must differ");
    }
    if (!asAdmin && !req.absentUserId().equals(actorId)) {
      throw new ForbiddenException("You may only register acting coverage for yourself");
    }

    AppUserEntity absent =
        appUserRepository
            .findByIdAndDeletedAtIsNull(req.absentUserId())
            .orElseThrow(() -> new NotFoundException("Absent user not found"));
    AppUserEntity acting =
        appUserRepository
            .findByIdAndDeletedAtIsNull(req.actingUserId())
            .orElseThrow(() -> new NotFoundException("Acting user not found"));
    if (!Boolean.TRUE.equals(absent.getActive()) || !Boolean.TRUE.equals(acting.getActive())) {
      throw new BadRequestException("Both users must be active");
    }

    assertActingAtLeastAsClearedAsAbsent(acting, absent);

    DepartmentEntity dept = null;
    if (req.departmentId() != null) {
      dept =
          departmentRepository
              .findByIdAndDeletedAtIsNull(req.departmentId())
              .orElseThrow(() -> new BadRequestException("Unknown department"));
    }

    assertNoDuplicateActiveRow(req.absentUserId(), req.departmentId(), today);

    ActingAssignmentEntity e = new ActingAssignmentEntity();
    e.setAbsentUser(absent);
    e.setActingUser(acting);
    e.setDepartment(dept);
    e.setIncludeDepartmentSubtree(Boolean.TRUE.equals(req.includeDepartmentSubtree()));
    e.setOrgLevelCode(trimToNull(req.orgLevelCode()));
    e.setCorrespondenceType(resolveType(req.correspondenceTypeId()));
    e.setConfidentiality(resolveConf(req.confidentialityId()));
    e.setWorkflowActionType(resolveWfAction(req.workflowActionTypeId()));
    e.setProcessDefinitionKey(trimToNull(req.processDefinitionKey()));
    e.setTaskDefinitionKey(trimToNull(req.taskDefinitionKey()));
    e.setValidFrom(req.validFrom());
    e.setValidTo(req.validTo());
    e.setNotes(trimToNull(req.notes()));
    e.setCreatedBy(actorId);
    e.setUpdatedBy(actorId);

    ActingAssignmentEntity saved = actingAssignmentRepository.save(e);
    emitAudit(actorId, ACTION_CREATED, saved);
    log.info(
        "Acting assignment created id={} absent={} acting={} validFrom={} validTo={}",
        saved.getId(),
        absent.getId(),
        acting.getId(),
        saved.getValidFrom(),
        saved.getValidTo());
    return toDto(saved, today);
  }

  @Transactional
  public void revoke(UUID actorId, UUID assignmentId, boolean asAdmin) {
    ActingAssignmentEntity row =
        actingAssignmentRepository
            .findByIdAndRevokedAtIsNull(assignmentId)
            .orElseThrow(() -> new NotFoundException("Acting assignment not found or already revoked"));
    boolean absentSelf = row.getAbsentUser().getId().equals(actorId);
    if (!absentSelf && !asAdmin) {
      throw new ForbiddenException("Only the absent user or an administrator may revoke");
    }
    row.setRevokedAt(Instant.now());
    row.setRevokedBy(actorId);
    row.setUpdatedBy(actorId);
    actingAssignmentRepository.save(row);
    emitAudit(actorId, ACTION_REVOKED, row);
  }

  @Transactional
  public int expireOverdue(LocalDate today) {
    List<ActingAssignmentEntity> overdue = actingAssignmentRepository.findExpiredNotRevoked(today);
    int n = 0;
    Instant now = Instant.now();
    for (ActingAssignmentEntity row : overdue) {
      if (row.getRevokedAt() != null) {
        continue;
      }
      row.setRevokedAt(now);
      row.setRevokedBy(null);
      row.setUpdatedBy(null);
      actingAssignmentRepository.save(row);
      emitAudit(null, ACTION_EXPIRED, row);
      n++;
    }
    if (n > 0) {
      log.info("ActingAssignmentService expired {} acting row(s) on {}", n, today);
    }
    return n;
  }

  @Transactional
  public void recordAssignmentUsed(ActingAssignmentEntity row, String camundaTaskId, UUID correspondenceId) {
    if (row == null) {
      return;
    }
    auditTrailService.append(
        new CreateAuditEventRequestDto(
            row.getActingUser().getId().toString(),
            ACTION_USED,
            RESOURCE_TYPE,
            row.getId().toString(),
            "{\"camundaTaskId\":\""
                + safe(camundaTaskId)
                + "\",\"correspondenceId\":\""
                + (correspondenceId != null ? correspondenceId.toString() : "")
                + "\"}",
            null,
            null,
            Instant.now()));
  }

  @Transactional(readOnly = true)
  public Optional<ActingAssignmentEntity> findBestMatchForTask(
      UUID absentUserId,
      CorrespondenceEntity correspondence,
      String processDefinitionKey,
      String taskDefinitionKey,
      Long workflowActionTypeId) {
    return actingAssignmentMatcher.findBestMatch(
        absentUserId, correspondence, processDefinitionKey, taskDefinitionKey, workflowActionTypeId);
  }

  @Transactional(readOnly = true)
  public boolean isActingClearedForCorrespondence(UUID actingUserId, CorrespondenceEntity correspondence) {
    if (actingUserId == null || correspondence == null) {
      return false;
    }
    return slaClearanceFilter.isCleared(correspondence, actingUserId);
  }

  private void assertNoDuplicateActiveRow(UUID absentId, Long departmentId, LocalDate today) {
    List<ActingAssignmentEntity> active = actingAssignmentRepository.findActiveByAbsentUser(absentId, today);
    Long deptKey = departmentId;
    boolean dup =
        active.stream()
            .anyMatch(
                a -> {
                  Long aDept = a.getDepartment() == null ? null : a.getDepartment().getId();
                  return Objects.equals(aDept, deptKey);
                });
    if (dup) {
      throw new BadRequestException(
          "An active acting assignment already exists for this absent user and department scope");
    }
  }

  private void assertActingAtLeastAsClearedAsAbsent(AppUserEntity acting, AppUserEntity absent) {
    int actingOrder = clearanceSortOrder(acting.getSecurityClearanceId());
    int absentOrder = clearanceSortOrder(absent.getSecurityClearanceId());
    if (actingOrder > absentOrder) {
      throw new ForbiddenException(
          "Acting user clearance is lower than the absent user; acting coverage would bypass confidentiality.");
    }
  }

  private int clearanceSortOrder(Long clearanceId) {
    if (clearanceId == null) {
      return Integer.MAX_VALUE;
    }
    return confidentialityRepository
        .findByIdAndDeletedAtIsNull(clearanceId)
        .map(c -> c.getSortOrder() == null ? Integer.MAX_VALUE : c.getSortOrder())
        .orElse(Integer.MAX_VALUE);
  }

  private CorrespondenceTypeEntity resolveType(Long id) {
    if (id == null) {
      return null;
    }
    return correspondenceTypeRepository
        .findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new BadRequestException("Unknown correspondence type id"));
  }

  private ConfidentialityEntity resolveConf(Long id) {
    if (id == null) {
      return null;
    }
    return confidentialityRepository
        .findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new BadRequestException("Unknown confidentiality id"));
  }

  private WorkflowActionTypeEntity resolveWfAction(Long id) {
    if (id == null) {
      return null;
    }
    return workflowActionTypeRepository
        .findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new BadRequestException("Unknown workflow action type id"));
  }

  private static String trimToNull(String s) {
    if (!StringUtils.hasText(s)) {
      return null;
    }
    return s.trim();
  }

  private static String safe(String s) {
    return s == null ? "" : s.replace("\"", "\\\"");
  }

  private void emitAudit(UUID actorId, String action, ActingAssignmentEntity row) {
    auditTrailService.append(
        new CreateAuditEventRequestDto(
            actorId != null ? actorId.toString() : "SYSTEM",
            action,
            RESOURCE_TYPE,
            row.getId().toString(),
            "{\"absentUserId\":\""
                + row.getAbsentUser().getId()
                + "\",\"actingUserId\":\""
                + row.getActingUser().getId()
                + "\"}",
            null,
            null,
            Instant.now()));
  }

  private static ActingAssignmentDto toDto(ActingAssignmentEntity e, LocalDate today) {
    return new ActingAssignmentDto(
        e.getId(),
        e.getAbsentUser().getId(),
        e.getAbsentUser().getUsername(),
        e.getActingUser().getId(),
        e.getActingUser().getUsername(),
        e.getDepartment() == null ? null : e.getDepartment().getId(),
        e.isIncludeDepartmentSubtree(),
        e.getOrgLevelCode(),
        e.getCorrespondenceType() == null ? null : e.getCorrespondenceType().getId(),
        e.getConfidentiality() == null ? null : e.getConfidentiality().getId(),
        e.getWorkflowActionType() == null ? null : e.getWorkflowActionType().getId(),
        e.getProcessDefinitionKey(),
        e.getTaskDefinitionKey(),
        e.getValidFrom(),
        e.getValidTo(),
        e.getNotes(),
        e.getRevokedAt(),
        e.isActiveOn(today),
        lifecycleStatus(e, today));
  }

  private static String lifecycleStatus(ActingAssignmentEntity e, LocalDate today) {
    if (e.getRevokedAt() != null) {
      return "REVOKED";
    }
    if (today.isBefore(e.getValidFrom())) {
      return "UPCOMING";
    }
    if (today.isAfter(e.getValidTo())) {
      return "EXPIRED";
    }
    return "ACTIVE";
  }
}
