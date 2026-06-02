package com.gov.ac.feature.delegation.task.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.ForbiddenException;
import com.gov.ac.feature.audit.dto.CreateAuditEventRequestDto;
import com.gov.ac.feature.audit.service.AuditTrailService;
import com.gov.ac.feature.delegation.repository.AuthorityDelegationRepository;
import com.gov.ac.feature.delegation.task.dto.CreateTaskDelegationRequestDto;
import com.gov.ac.feature.delegation.task.dto.TaskDelegationDto;
import com.gov.ac.feature.delegation.task.entity.TaskDelegationEntity;
import com.gov.ac.feature.delegation.task.repository.TaskDelegationRepository;
import com.gov.ac.feature.lookups.entity.ConfidentialityEntity;
import com.gov.ac.feature.lookups.repository.ConfidentialityRepository;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskDelegationServiceTest {

  @Mock private TaskDelegationRepository taskDelegationRepository;
  @Mock private AuthorityDelegationRepository authorityDelegationRepository;
  @Mock private AppUserRepository appUserRepository;
  @Mock private ConfidentialityRepository confidentialityRepository;
  @Mock private AuditTrailService auditTrailService;

  @InjectMocks private TaskDelegationService service;

  private UUID delegatorId;
  private UUID delegateId;
  private AppUserEntity delegator;
  private AppUserEntity delegate;

  @BeforeEach
  void setUp() {
    delegatorId = UUID.randomUUID();
    delegateId = UUID.randomUUID();
    delegator = activeUser(delegatorId);
    delegate = activeUser(delegateId);
    lenient().when(appUserRepository.findByIdAndDeletedAtIsNull(delegatorId))
        .thenReturn(Optional.of(delegator));
    lenient().when(appUserRepository.findByIdAndDeletedAtIsNull(delegateId))
        .thenReturn(Optional.of(delegate));
    lenient().when(taskDelegationRepository.findOverlappingByDelegator(any(), any(), any()))
        .thenReturn(List.of());
    lenient().when(taskDelegationRepository.findActiveByDelegator(any(), any())).thenReturn(List.of());
    lenient().when(taskDelegationRepository.save(any(TaskDelegationEntity.class)))
        .thenAnswer(inv -> {
          TaskDelegationEntity row = inv.getArgument(0);
          if (row.getId() == null) {
            row.setId(UUID.randomUUID());
          }
          return row;
        });
  }

  // ---------------------------------------------------------------------------
  // Create / Revoke
  // ---------------------------------------------------------------------------

  @Test
  void createPersistsRowAndEmitsAuditEvent() {
    CreateTaskDelegationRequestDto req = taskScopedRequest(LocalDate.now(), LocalDate.now().plusDays(3));

    TaskDelegationDto dto = service.create(delegatorId, req);

    assertThat(dto).isNotNull();
    assertThat(dto.active()).isTrue();
    assertThat(dto.delegate().getId()).isEqualTo(delegateId);
    assertThat(dto.scopeType()).isEqualTo(TaskDelegationEntity.SCOPE_TASK);
    verify(auditTrailService)
        .append(
            argThat(
                (CreateAuditEventRequestDto evt) ->
                    TaskDelegationService.ACTION_CREATED.equals(evt.actionCode())
                        && delegatorId.toString().equals(evt.actorUserId())
                        && TaskDelegationService.RESOURCE_TYPE.equals(evt.resourceType())));
  }

  @Test
  void createRejectsSelfDelegation() {
    CreateTaskDelegationRequestDto req =
        new CreateTaskDelegationRequestDto(
            delegatorId,
            TaskDelegationEntity.SCOPE_TASK,
            null,
            "task-1",
            null,
            null,
            null,
            LocalDate.now(),
            LocalDate.now().plusDays(1),
            null,
            null);

    assertThatThrownBy(() -> service.create(delegatorId, req))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("yourself");
  }

  @Test
  void createRejectsInvertedDateWindow() {
    CreateTaskDelegationRequestDto req =
        new CreateTaskDelegationRequestDto(
            delegateId,
            TaskDelegationEntity.SCOPE_TASK,
            null,
            "task-1",
            null,
            null,
            null,
            LocalDate.now().plusDays(5),
            LocalDate.now(),
            null,
            null);

    assertThatThrownBy(() -> service.create(delegatorId, req))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("validTo");
  }

  @Test
  void createRevokeRoundtripSetsRevokedAtAndEmitsRevokedAudit() {
    TaskDelegationEntity row = newSavedRow();
    when(taskDelegationRepository.findByIdAndRevokedAtIsNull(row.getId())).thenReturn(Optional.of(row));

    service.revoke(delegatorId, row.getId(), false);

    assertThat(row.getRevokedAt()).isNotNull();
    assertThat(row.getRevokedBy()).isEqualTo(delegatorId);
    verify(auditTrailService)
        .append(
            argThat(
                (CreateAuditEventRequestDto evt) ->
                    TaskDelegationService.ACTION_REVOKED.equals(evt.actionCode())));
  }

  @Test
  void revokeByNonDelegatorWithoutAdminPermissionIsForbidden() {
    TaskDelegationEntity row = newSavedRow();
    when(taskDelegationRepository.findByIdAndRevokedAtIsNull(row.getId())).thenReturn(Optional.of(row));
    UUID stranger = UUID.randomUUID();

    assertThatThrownBy(() -> service.revoke(stranger, row.getId(), false))
        .isInstanceOf(ForbiddenException.class);
  }

  // ---------------------------------------------------------------------------
  // Overlap, cycle, clearance
  // ---------------------------------------------------------------------------

  @Test
  void createRejectsOverlappingDuplicateScope() {
    TaskDelegationEntity existing = newSavedRow();
    existing.setCamundaTaskId("task-1");
    when(taskDelegationRepository.findOverlappingByDelegator(
            eq(delegatorId), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(List.of(existing));

    CreateTaskDelegationRequestDto req = taskScopedRequest(LocalDate.now(), LocalDate.now().plusDays(2));

    assertThatThrownBy(() -> service.create(delegatorId, req))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("active delegation");
  }

  @Test
  void createWithOverlappingDifferentScopeIsAllowed() {
    TaskDelegationEntity existing = newSavedRow();
    existing.setCamundaTaskId("OTHER-TASK");
    when(taskDelegationRepository.findOverlappingByDelegator(
            eq(delegatorId), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(List.of(existing));

    CreateTaskDelegationRequestDto req = taskScopedRequest(LocalDate.now(), LocalDate.now().plusDays(2));

    TaskDelegationDto dto = service.create(delegatorId, req);
    assertThat(dto.camundaTaskId()).isEqualTo("task-1");
  }

  @Test
  void createDetectsTwoStepCycle() {
    // delegate -> delegator already exists; new delegator -> delegate would close the loop.
    TaskDelegationEntity reverse = new TaskDelegationEntity();
    reverse.setId(UUID.randomUUID());
    reverse.setDelegatorUser(delegate);
    reverse.setDelegateUser(delegator);
    reverse.setScopeType(TaskDelegationEntity.SCOPE_TASK);
    reverse.setValidFrom(LocalDate.now().minusDays(1));
    reverse.setValidTo(LocalDate.now().plusDays(7));
    when(taskDelegationRepository.findActiveByDelegator(eq(delegateId), any(LocalDate.class)))
        .thenReturn(List.of(reverse));

    CreateTaskDelegationRequestDto req = taskScopedRequest(LocalDate.now(), LocalDate.now().plusDays(3));

    assertThatThrownBy(() -> service.create(delegatorId, req))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("circular");
  }

  @Test
  void createRejectsLowerClearanceDelegate() {
    delegator.setSecurityClearanceId(10L); // TOP_SECRET ish (low sort_order = restrictive)
    delegate.setSecurityClearanceId(50L); // NORMAL
    when(confidentialityRepository.findByIdAndDeletedAtIsNull(10L))
        .thenReturn(Optional.of(confidentialityLevel(10, "TOP_SECRET", true)));
    when(confidentialityRepository.findByIdAndDeletedAtIsNull(50L))
        .thenReturn(Optional.of(confidentialityLevel(50, "NORMAL", false)));
    CreateTaskDelegationRequestDto req = taskScopedRequest(LocalDate.now(), LocalDate.now().plusDays(2));

    assertThatThrownBy(() -> service.create(delegatorId, req))
        .isInstanceOf(ForbiddenException.class)
        .hasMessageContaining("clearance");
  }

  // ---------------------------------------------------------------------------
  // Expiry idempotency
  // ---------------------------------------------------------------------------

  @Test
  void expireOverdueIsIdempotent() {
    TaskDelegationEntity overdue = newSavedRow();
    overdue.setValidTo(LocalDate.now().minusDays(1));
    when(taskDelegationRepository.findExpiredAsOf(any(LocalDate.class)))
        .thenReturn(List.of(overdue))
        .thenReturn(List.of()); // second pass — nothing to do

    int first = service.expireOverdue(LocalDate.now());
    int second = service.expireOverdue(LocalDate.now());

    assertThat(first).isEqualTo(1);
    assertThat(second).isZero();
    assertThat(overdue.getRevokedAt()).isNotNull();
    // Exactly one TASK_DELEGATION_EXPIRED was emitted across both runs.
    verify(auditTrailService, times(1))
        .append(
            argThat(
                (CreateAuditEventRequestDto evt) ->
                    TaskDelegationService.ACTION_EXPIRED.equals(evt.actionCode())));
  }

  // ---------------------------------------------------------------------------
  // Effective assignment resolution + audit on use
  // ---------------------------------------------------------------------------

  @Test
  void findEffectiveDelegationForTaskPrefersTaskScopedRowOverBroadScope() {
    TaskDelegationEntity taskScoped = newSavedRow();
    taskScoped.setCamundaTaskId("task-1");

    when(taskDelegationRepository.findActiveTaskScoped(
            eq(delegatorId), eq("task-1"), any(), any(LocalDate.class)))
        .thenReturn(List.of(taskScoped));

    Optional<TaskDelegationEntity> match =
        service.findEffectiveDelegationForTask(delegatorId, "task-1", null, "INCOMING", "NORMAL");

    assertThat(match).isPresent();
    assertThat(match.get().getId()).isEqualTo(taskScoped.getId());
    // Broad-scope query should NOT have been hit because the task-scoped match won.
    verify(taskDelegationRepository, never()).findActiveByDelegator(eq(delegatorId), any(LocalDate.class));
  }

  @Test
  void findEffectiveDelegationFallsBackToTypeConfidentialityScope() {
    TaskDelegationEntity broad = newSavedRow();
    broad.setScopeType(TaskDelegationEntity.SCOPE_TYPE_CONFIDENTIALITY);
    broad.setAllowedCorrespondenceTypeCodes("INCOMING");
    broad.setAllowedConfidentialityCodes("NORMAL");

    when(taskDelegationRepository.findActiveTaskScoped(eq(delegatorId), any(), any(), any()))
        .thenReturn(List.of());
    when(taskDelegationRepository.findActiveByDelegator(eq(delegatorId), any(LocalDate.class)))
        .thenReturn(List.of(broad));

    Optional<TaskDelegationEntity> match =
        service.findEffectiveDelegationForTask(
            delegatorId, "task-1", UUID.randomUUID(), "INCOMING", "NORMAL");

    assertThat(match).contains(broad);
  }

  @Test
  void findEffectiveDelegationDoesNotMatchWhenCsvFilterExcludesCode() {
    TaskDelegationEntity broad = newSavedRow();
    broad.setScopeType(TaskDelegationEntity.SCOPE_TYPE_CONFIDENTIALITY);
    broad.setAllowedConfidentialityCodes("NORMAL");

    when(taskDelegationRepository.findActiveTaskScoped(eq(delegatorId), any(), any(), any()))
        .thenReturn(List.of());
    when(taskDelegationRepository.findActiveByDelegator(eq(delegatorId), any(LocalDate.class)))
        .thenReturn(List.of(broad));

    Optional<TaskDelegationEntity> match =
        service.findEffectiveDelegationForTask(
            delegatorId, null, UUID.randomUUID(), "INCOMING", "TOP_SECRET");

    assertThat(match).isEmpty();
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private CreateTaskDelegationRequestDto taskScopedRequest(LocalDate from, LocalDate to) {
    return new CreateTaskDelegationRequestDto(
        delegateId, TaskDelegationEntity.SCOPE_TASK, null, "task-1", null, null, null, from, to, null, null);
  }

  private TaskDelegationEntity newSavedRow() {
    TaskDelegationEntity row = new TaskDelegationEntity();
    row.setId(UUID.randomUUID());
    row.setDelegatorUser(delegator);
    row.setDelegateUser(delegate);
    row.setScopeType(TaskDelegationEntity.SCOPE_TASK);
    row.setCamundaTaskId("task-1");
    row.setValidFrom(LocalDate.now());
    row.setValidTo(LocalDate.now().plusDays(2));
    return row;
  }

  private static AppUserEntity activeUser(UUID id) {
    AppUserEntity user = new AppUserEntity();
    user.setId(id);
    user.setActive(true);
    return user;
  }

  private static ConfidentialityEntity confidentialityLevel(int sort, String code, boolean requires) {
    ConfidentialityEntity level = new ConfidentialityEntity();
    level.setId((long) sort);
    level.setCode(code);
    level.setSortOrder(sort);
    level.setRequiresClearance(requires);
    level.setActive(true);
    return level;
  }
}
