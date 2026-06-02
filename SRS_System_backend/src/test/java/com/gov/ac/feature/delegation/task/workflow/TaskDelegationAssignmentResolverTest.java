package com.gov.ac.feature.delegation.task.workflow;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gov.ac.feature.acting.entity.ActingAssignmentEntity;
import com.gov.ac.feature.acting.service.ActingAssignmentService;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.feature.correspondence.workflow.CorrespondenceWorkflowVariables;
import com.gov.ac.feature.delegation.task.entity.TaskDelegationEntity;
import com.gov.ac.feature.delegation.task.service.TaskDelegationService;
import com.gov.ac.feature.lookups.entity.ConfidentialityEntity;
import com.gov.ac.feature.lookups.entity.CorrespondenceTypeEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;
import java.util.Optional;
import java.util.UUID;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskDelegationAssignmentResolverTest {

  @Mock private TaskDelegationService taskDelegationService;
  @Mock private ActingAssignmentService actingAssignmentService;
  @Mock private CorrespondenceRepository correspondenceRepository;
  @Mock private TaskService taskService;
  @Mock private DelegateTask task;

  @InjectMocks private TaskDelegationAssignmentResolver resolver;

  private UUID delegatorId;
  private UUID delegateId;
  private UUID correspondenceId;
  private TaskDelegationEntity delegation;

  @BeforeEach
  void setUp() {
    delegatorId = UUID.randomUUID();
    delegateId = UUID.randomUUID();
    correspondenceId = UUID.randomUUID();
    delegation = new TaskDelegationEntity();
    delegation.setId(UUID.randomUUID());
    AppUserEntity delegator = new AppUserEntity();
    delegator.setId(delegatorId);
    AppUserEntity delegate = new AppUserEntity();
    delegate.setId(delegateId);
    delegation.setDelegatorUser(delegator);
    delegation.setDelegateUser(delegate);
    delegation.setScopeType(TaskDelegationEntity.SCOPE_TASK);

    lenient()
        .when(actingAssignmentService.findBestMatchForTask(any(), any(), any(), any(), any()))
        .thenReturn(Optional.empty());
    lenient().when(task.getProcessDefinitionId()).thenReturn("inbound-correspondence:1:def");
    lenient().when(task.getTaskDefinitionKey()).thenReturn("approveTask");
  }

  @Test
  void rewiresTaskAndStashesLocalsWhenDelegationFound() {
    when(task.getId()).thenReturn("task-1");
    when(task.getVariable(CorrespondenceWorkflowVariables.CORRESPONDENCE_ID))
        .thenReturn(correspondenceId.toString());

    CorrespondenceEntity correspondence = new CorrespondenceEntity();
    CorrespondenceTypeEntity type = new CorrespondenceTypeEntity();
    type.setCode("INCOMING");
    correspondence.setCorrespondenceType(type);
    ConfidentialityEntity conf = new ConfidentialityEntity();
    conf.setCode("NORMAL");
    correspondence.setConfidentiality(conf);
    when(correspondenceRepository.findById(correspondenceId)).thenReturn(Optional.of(correspondence));

    when(taskDelegationService.findEffectiveDelegationForTask(
            eq(delegatorId), eq("task-1"), eq(correspondenceId), eq("INCOMING"), eq("NORMAL")))
        .thenReturn(Optional.of(delegation));

    lenient().when(task.getAssignee()).thenReturn(delegatorId.toString());

    resolver.resolveAndApply(task, delegatorId.toString());

    verify(task).setVariableLocal(
        CorrespondenceWorkflowVariables.WORKFLOW_DIRECT_ASSIGNEE_USER_ID, delegatorId.toString());
    verify(task).setAssignee(delegateId.toString());
    verify(task)
        .setVariableLocal(
            CorrespondenceWorkflowVariables.ORIGINAL_ASSIGNEE_USER_ID, delegatorId.toString());
    verify(task)
        .setVariableLocal(
            CorrespondenceWorkflowVariables.ACTING_DELEGATE_USER_ID, delegateId.toString());
    verify(task)
        .setVariableLocal(
            CorrespondenceWorkflowVariables.TASK_DELEGATION_ID, delegation.getId().toString());
    verify(taskDelegationService, times(1))
        .recordTaskRoutedToDelegate(delegation, "task-1", correspondenceId);
  }

  @Test
  void actingOverlayRunsBeforeDelegation() {
    UUID actingUserId = UUID.randomUUID();
    when(task.getId()).thenReturn("task-2");
    when(task.getVariable(CorrespondenceWorkflowVariables.CORRESPONDENCE_ID))
        .thenReturn(correspondenceId.toString());
    CorrespondenceEntity correspondence = new CorrespondenceEntity();
    correspondence.setId(correspondenceId);
    CorrespondenceTypeEntity type = new CorrespondenceTypeEntity();
    type.setCode("INCOMING");
    correspondence.setCorrespondenceType(type);
    ConfidentialityEntity conf = new ConfidentialityEntity();
    conf.setCode("NORMAL");
    correspondence.setConfidentiality(conf);
    when(correspondenceRepository.findById(correspondenceId)).thenReturn(Optional.of(correspondence));

    ActingAssignmentEntity acting = new ActingAssignmentEntity();
    acting.setId(UUID.randomUUID());
    AppUserEntity actingUser = new AppUserEntity();
    actingUser.setId(actingUserId);
    acting.setActingUser(actingUser);
    AppUserEntity absent = new AppUserEntity();
    absent.setId(delegatorId);
    acting.setAbsentUser(absent);

    when(actingAssignmentService.findBestMatchForTask(
            eq(delegatorId), eq(correspondence), eq("inbound-correspondence"), eq("approveTask"), eq(null)))
        .thenReturn(Optional.of(acting));
    when(actingAssignmentService.isActingClearedForCorrespondence(actingUserId, correspondence))
        .thenReturn(true);

    when(task.getAssignee()).thenReturn(actingUserId.toString());

    when(taskDelegationService.findEffectiveDelegationForTask(
            eq(actingUserId), eq("task-2"), eq(correspondenceId), eq("INCOMING"), eq("NORMAL")))
        .thenReturn(Optional.of(delegation));

    resolver.resolveAndApply(task, delegatorId.toString());

    verify(task).setAssignee(actingUserId.toString());
    verify(task).setAssignee(delegateId.toString());
    verify(task)
        .setVariableLocal(CorrespondenceWorkflowVariables.ORIGINAL_ASSIGNEE_USER_ID, actingUserId.toString());
  }

  @Test
  void leavesTaskWithoutDelegationWhenNoMatch() {
    when(task.getId()).thenReturn("task-1");
    when(task.getVariable(CorrespondenceWorkflowVariables.CORRESPONDENCE_ID)).thenReturn(null);
    when(taskDelegationService.findEffectiveDelegationForTask(
            eq(delegatorId), any(), any(), any(), any()))
        .thenReturn(Optional.empty());
    when(task.getAssignee()).thenReturn(delegatorId.toString());

    resolver.resolveAndApply(task, delegatorId.toString());

    verify(task).setVariableLocal(
        CorrespondenceWorkflowVariables.WORKFLOW_DIRECT_ASSIGNEE_USER_ID, delegatorId.toString());
    verify(task, never()).setAssignee(any());
    verify(task, never())
        .setVariableLocal(eq(CorrespondenceWorkflowVariables.ORIGINAL_ASSIGNEE_USER_ID), any());
    verify(taskDelegationService, never()).recordTaskRoutedToDelegate(any(), any(), any());
  }

  @Test
  void silentlySkipsWhenAssigneeIsNotUuid() {
    resolver.resolveAndApply(task, "DEPT_42");

    verify(taskDelegationService, never()).findEffectiveDelegationForTask(any(), any(), any(), any(), any());
    verify(task, never()).setAssignee(any());
  }
}
