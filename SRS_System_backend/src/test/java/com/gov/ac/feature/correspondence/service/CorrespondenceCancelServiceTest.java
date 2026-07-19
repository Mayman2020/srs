package com.gov.ac.feature.correspondence.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gov.ac.feature.correspondence.audit.CorrespondenceActionAudit;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.feature.correspondence.security.CorrespondenceViewAuthorization;
import com.gov.ac.feature.lookups.entity.CorrespondenceStatusEntity;
import com.gov.ac.feature.lookups.repository.CorrespondenceStatusRepository;
import com.gov.ac.feature.lookups.repository.WorkflowInstanceStatusRepository;
import com.gov.ac.feature.retention.LegalHoldService;
import com.gov.ac.feature.shared.lookup.service.LookupResolutionService;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import com.gov.ac.feature.workflow.execution.entity.WorkflowHistoryEntity;
import com.gov.ac.feature.workflow.execution.entity.WorkflowInstanceEntity;
import com.gov.ac.feature.workflow.execution.repository.WorkflowHistoryRepository;
import com.gov.ac.feature.workflow.execution.repository.WorkflowInstanceRepository;
import com.gov.ac.feature.workflow.execution.service.WorkflowService;
import com.gov.ac.security.permission.EffectiveUserPermissionService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CorrespondenceCancelServiceTest {

  @Mock CorrespondenceRepository correspondenceRepository;
  @Mock CorrespondenceStatusRepository correspondenceStatusRepository;
  @Mock AppUserRepository appUserRepository;
  @Mock CorrespondenceViewAuthorization correspondenceViewAuthorization;
  @Mock WorkflowInstanceRepository workflowInstanceRepository;
  @Mock WorkflowInstanceStatusRepository workflowInstanceStatusRepository;
  @Mock WorkflowHistoryRepository workflowHistoryRepository;
  @Mock LookupResolutionService lookups;
  @Mock WorkflowService workflowService;
  @Mock CorrespondenceActionAudit correspondenceActionAudit;
  @Mock EffectiveUserPermissionService effectiveUserPermissionService;
  @Mock LegalHoldService legalHoldService;

  @InjectMocks CorrespondenceCancelService service;

  @Test
  void camundaDeletionFailureAbortsCancellationBeforeHistoryOrAuditIsWritten() {
    UUID actorId = UUID.randomUUID();
    UUID correspondenceId = UUID.randomUUID();
    AppUserEntity actor = new AppUserEntity();
    actor.setActive(true);

    CorrespondenceStatusEntity open = new CorrespondenceStatusEntity();
    open.setCode("IN_PROGRESS");
    open.setTerminal(false);
    open.setAllowsCancel(true);
    CorrespondenceStatusEntity cancelled = new CorrespondenceStatusEntity();
    cancelled.setCode("CANCELLED");

    CorrespondenceEntity correspondence = new CorrespondenceEntity();
    correspondence.setId(correspondenceId);
    correspondence.setCorrespondenceStatus(open);

    WorkflowInstanceEntity instance = new WorkflowInstanceEntity();
    instance.setProcessInstanceId("camunda-instance-1");

    when(appUserRepository.findByIdAndDeletedAtIsNull(actorId)).thenReturn(Optional.of(actor));
    when(effectiveUserPermissionService.hasActivePermission(actorId, "CORRESPONDENCE_DELETE"))
        .thenReturn(true);
    when(correspondenceRepository.findByIdAndDeletedAtIsNullWithOwnerDepartment(correspondenceId))
        .thenReturn(Optional.of(correspondence));
    when(correspondenceStatusRepository.findByCancelOutcomeTrueAndActiveTrueAndDeletedAtIsNull())
        .thenReturn(Optional.of(cancelled));
    when(workflowInstanceRepository.findByCorrespondence_IdAndDeletedAtIsNullOrderByStartedAtDesc(
            correspondenceId))
        .thenReturn(List.of(instance));
    when(workflowService.hasActiveProcessInstance("camunda-instance-1")).thenReturn(true);
    doThrow(new IllegalStateException("engine unavailable"))
        .when(workflowService)
        .deleteProcessInstance("camunda-instance-1", "CORRESPONDENCE_CANCELLED");

    assertThatThrownBy(() -> service.cancel(correspondenceId, actorId, null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("engine unavailable");

    verify(workflowHistoryRepository, never()).save(any(WorkflowHistoryEntity.class));
    verify(correspondenceActionAudit, never()).log(any(), any(), any(), any());
  }
}
