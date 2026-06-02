package com.gov.ac.feature.correspondence.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.feature.attachment.entity.AttachmentEntity;
import com.gov.ac.feature.attachment.repository.AttachmentRepository;
import com.gov.ac.feature.attachment.signature.DocumentSignatureService;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.feature.correspondence.security.CorrespondenceViewAuthorization;
import com.gov.ac.feature.correspondence.workflow.CorrespondenceCamundaTaskSupport;
import com.gov.ac.feature.correspondence.workflow.WorkflowActionResolutionService;
import com.gov.ac.feature.lookups.entity.CorrespondenceStatusEntity;
import com.gov.ac.feature.lookups.entity.WorkflowActionTypeEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import com.gov.ac.feature.workflow.execution.service.WorkflowService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CorrespondenceWorkflowActionServiceSignatureTest {

  @Mock private CorrespondenceRepository correspondenceRepository;
  @Mock private AppUserRepository appUserRepository;
  @Mock private CorrespondenceViewAuthorization correspondenceViewAuthorization;
  @Mock private WorkflowService workflowService;
  @Mock private CorrespondenceCamundaTaskSupport camundaTaskSupport;
  @Mock private WorkflowActionResolutionService workflowActionResolution;
  @Mock private AttachmentRepository attachmentRepository;
  @Mock private DocumentSignatureService documentSignatureService;

  @InjectMocks private CorrespondenceWorkflowActionService service;

  private UUID userId;
  private UUID correspondenceId;
  private AttachmentEntity attachment;
  private WorkflowActionTypeEntity rule;
  private Task task;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    correspondenceId = UUID.randomUUID();

    AppUserEntity viewer = new AppUserEntity();
    viewer.setId(userId);
    viewer.setActive(true);

    CorrespondenceStatusEntity status = new CorrespondenceStatusEntity();
    status.setId(7L);
    status.setTerminal(false);

    CorrespondenceEntity correspondence = new CorrespondenceEntity();
    correspondence.setId(correspondenceId);
    correspondence.setCorrespondenceStatus(status);
    correspondence.setReferenceNumber("REF-1");

    attachment = new AttachmentEntity();
    attachment.setId(101L);
    attachment.setCorrespondence(correspondence);
    attachment.setCurrentVersionId(202L);

    rule = new WorkflowActionTypeEntity();
    rule.setCode("APPROVE");
    rule.setRequiresComment(false);
    rule.setRequiresSignature(true);

    task = org.mockito.Mockito.mock(Task.class);
    when(task.getId()).thenReturn("task-1");
    when(task.getAssignee()).thenReturn(userId.toString());

    when(appUserRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(viewer));
    when(correspondenceRepository.findByIdAndDeletedAtIsNullWithOwnerDepartment(correspondenceId))
        .thenReturn(Optional.of(correspondence));
    when(camundaTaskSupport.findActiveTasksForUser("REF-1", userId)).thenReturn(List.of(task));
    when(workflowActionResolution.resolveTransition("APPROVE", 7L)).thenReturn(Optional.of(rule));
  }

  @Test
  void rejectsWhenSignatureRequiredAndMissing() {
    when(attachmentRepository.findAllForDetailByCorrespondenceId(correspondenceId))
        .thenReturn(List.of(attachment));
    when(documentSignatureService.hasValidSignatureByUser(202L, userId)).thenReturn(false);

    assertThatThrownBy(
            () -> service.completeActiveAssigneeTask(correspondenceId, userId, "APPROVE", null))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Sign required attachments");

    verify(workflowService, never()).completeTask(anyString(), anyMap());
  }

  @Test
  void completesWhenSignatureRequiredAndPresent() {
    when(attachmentRepository.findAllForDetailByCorrespondenceId(correspondenceId))
        .thenReturn(List.of(attachment));
    when(documentSignatureService.hasValidSignatureByUser(202L, userId)).thenReturn(true);

    service.completeActiveAssigneeTask(correspondenceId, userId, "APPROVE", null);

    verify(workflowService, times(1)).completeTask(eq("task-1"), any());
  }

  @Test
  void skipsCheckWhenNoAttachments() {
    when(attachmentRepository.findAllForDetailByCorrespondenceId(correspondenceId))
        .thenReturn(List.of());
    service.completeActiveAssigneeTask(correspondenceId, userId, "APPROVE", null);
    verify(documentSignatureService, never()).hasValidSignatureByUser(anyLong(), any());
    verify(workflowService).completeTask(eq("task-1"), any());
  }

  @Test
  void skipsCheckWhenRuleDoesNotRequireSignature() {
    rule.setRequiresSignature(false);
    service.completeActiveAssigneeTask(correspondenceId, userId, "APPROVE", null);
    verify(attachmentRepository, never()).findAllForDetailByCorrespondenceId(any());
    verify(documentSignatureService, never()).hasValidSignatureByUser(anyLong(), any());
    verify(workflowService).completeTask(eq("task-1"), any());
  }
}
