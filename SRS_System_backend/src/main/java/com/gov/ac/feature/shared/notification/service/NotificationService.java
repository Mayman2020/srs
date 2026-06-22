package com.gov.ac.feature.shared.notification.service;

import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.workflow.CorrespondenceWorkflowVariables;
import com.gov.ac.feature.lookups.entity.WorkflowActionTypeEntity;
import com.gov.ac.feature.lookups.repository.WorkflowActionTypeRepository;
import com.gov.ac.feature.lookups.entity.NotificationEventTypeEntity;
import com.gov.ac.feature.notification.inbox.entity.InAppNotificationEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.shared.notification.NotificationEventCodes;
import com.gov.ac.feature.shared.notification.NotificationMessageKeys;
import com.gov.ac.feature.users.repository.AppUserRepository;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.feature.notification.channel.NotificationOutboxService;
import com.gov.ac.feature.notification.channel.NotificationRoutingProperties;
import com.gov.ac.feature.notification.inbox.repository.InAppNotificationRepository;
import com.gov.ac.feature.lookups.repository.NotificationEventTypeRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

  private final InAppNotificationRepository inAppNotificationRepository;
  private final NotificationEventTypeRepository notificationEventTypeRepository;
  private final CorrespondenceNotificationRecipientResolver recipientResolver;
  private final CorrespondenceRepository correspondenceRepository;
  private final AppUserRepository appUserRepository;
  private final NotificationOutboxService notificationOutboxService;
  private final NotificationRoutingProperties notificationRoutingProperties;
  private final WorkflowActionTypeRepository workflowActionTypeRepository;

  @Transactional
  public void notifyCorrespondenceCreated(CorrespondenceEntity correspondence, AppUserEntity actor) {
    Optional<NotificationEventTypeEntity> eventTypeOpt =
        notificationEventTypeRepository.findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(
            NotificationEventCodes.CORRESPONDENCE_CREATED);
    if (eventTypeOpt.isEmpty()) {
      log.error(
          "Missing notification_event_type: {}", NotificationEventCodes.CORRESPONDENCE_CREATED);
      return;
    }
    Set<UUID> recipients =
        recipientResolver.ownerDepartmentRecipientsExcluding(correspondence, actor.getId());
    if (recipients.isEmpty()) {
      log.debug(
          "No inbox recipients for correspondence created: correspondenceId={}",
          correspondence.getId());
      return;
    }
    Map<String, Object> params = baseCorrespondenceParams(correspondence);
    params.put("actorUserId", actor.getId().toString());
    persistForRecipients(
        recipients,
        eventTypeOpt.get(),
        correspondence,
        NotificationMessageKeys.CORRESPONDENCE_CREATED,
        params);
  }

  @Transactional
  public void notifyCommentAdded(CorrespondenceEntity correspondence, AppUserEntity author) {
    Optional<NotificationEventTypeEntity> eventTypeOpt =
        notificationEventTypeRepository.findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(
            NotificationEventCodes.COMMENT_ADDED);
    if (eventTypeOpt.isEmpty()) {
      log.error("Missing notification_event_type: {}", NotificationEventCodes.COMMENT_ADDED);
      return;
    }
    Set<UUID> recipients =
        recipientResolver.ownerDepartmentRecipientsExcluding(correspondence, author.getId());
    if (recipients.isEmpty()) {
      return;
    }
    Map<String, Object> params = baseCorrespondenceParams(correspondence);
    params.put("authorUserId", author.getId().toString());
    persistForRecipients(
        recipients,
        eventTypeOpt.get(),
        correspondence,
        NotificationMessageKeys.CORRESPONDENCE_COMMENT_ADDED,
        params);
  }

  @Transactional
  public void notifyWorkflowTaskCompleted(DelegateTask delegateTask) {
    if (!TaskListener.EVENTNAME_COMPLETE.equals(delegateTask.getEventName())) {
      return;
    }
    String corrIdStr = (String) delegateTask.getVariable("correspondenceId");
    String referenceNumber = (String) delegateTask.getVariable("referenceNumber");
    if (!StringUtils.hasText(corrIdStr)) {
      log.warn("Workflow notification skipped: missing correspondenceId variable");
      return;
    }
    UUID correspondenceId;
    try {
      correspondenceId = UUID.fromString(corrIdStr);
    } catch (IllegalArgumentException ex) {
      log.warn("Workflow notification skipped: invalid correspondenceId={}", corrIdStr);
      return;
    }
    CorrespondenceEntity correspondence =
        correspondenceRepository
            .findByIdAndDeletedAtIsNullWithOwnerDepartment(correspondenceId)
            .orElse(null);
    if (correspondence == null) {
      log.warn("Workflow notification skipped: correspondence not found id={}", correspondenceId);
      return;
    }

    String taskKey =
        delegateTask.getTaskDefinitionKey() != null ? delegateTask.getTaskDefinitionKey() : "";
    String taskName = delegateTask.getName() != null ? delegateTask.getName() : "";
    String wfDecision = (String) delegateTask.getVariable(CorrespondenceWorkflowVariables.WF_DECISION);
    String eventCode = resolveNotificationEventCode(wfDecision);
    if (!StringUtils.hasText(eventCode)) {
      log.debug("Workflow notification skipped: no notification_event_type for wfDecision={}", wfDecision);
      return;
    }

    Optional<NotificationEventTypeEntity> eventTypeOpt =
        notificationEventTypeRepository.findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(
            eventCode);
    if (eventTypeOpt.isEmpty()) {
      log.error("Missing notification_event_type: {}", eventCode);
      return;
    }

    UUID completedBy = null;
    if (StringUtils.hasText(delegateTask.getAssignee())) {
      try {
        completedBy = UUID.fromString(delegateTask.getAssignee());
      } catch (IllegalArgumentException ignored) {
        // Camunda assignee values are not guaranteed to be UUIDs.
      }
    }

    Set<UUID> recipients =
        recipientResolver.ownerDepartmentRecipientsExcluding(correspondence, completedBy);
    if (recipients.isEmpty()) {
      return;
    }

    Map<String, Object> params = baseCorrespondenceParams(correspondence);
    params.put("taskDefinitionKey", taskKey);
    params.put("taskName", taskName);
    params.put("notificationEventCode", eventCode);
    if (referenceNumber != null) {
      params.put("referenceNumber", referenceNumber);
    }
    if (completedBy != null) {
      params.put("completedByUserId", completedBy.toString());
    }

    persistForRecipients(
        recipients,
        eventTypeOpt.get(),
        correspondence,
        NotificationMessageKeys.WORKFLOW_TASK_COMPLETED,
        params);
  }

  private String resolveNotificationEventCode(String wfDecision) {
    if (!StringUtils.hasText(wfDecision)) {
      return NotificationEventCodes.ASSIGNED;
    }
    return workflowActionTypeRepository.findWildcardRulesForCode(wfDecision.trim()).stream()
        .findFirst()
        .map(WorkflowActionTypeEntity::getNotificationEventType)
        .filter(event -> event != null && StringUtils.hasText(event.getCode()))
        .map(event -> event.getCode())
        .orElse(null);
  }

  private Map<String, Object> baseCorrespondenceParams(CorrespondenceEntity correspondence) {
    Map<String, Object> params = new HashMap<>();
    params.put("correspondenceId", correspondence.getId().toString());
    params.put("referenceNumber", correspondence.getReferenceNumber());
    params.put("subject", correspondence.getSubject());
    if (correspondence.getCorrespondenceType() != null) {
      params.put("correspondenceTypeCode", correspondence.getCorrespondenceType().getCode());
    }
    return params;
  }

  private void persistForRecipients(
      Set<UUID> recipientIds,
      NotificationEventTypeEntity eventType,
      CorrespondenceEntity correspondence,
      String messageKey,
      Map<String, Object> messageParams) {
    if (notificationRoutingProperties.isOutbox()) {
      for (UUID recipientId : recipientIds) {
        notificationOutboxService.enqueueInApp(
            recipientId,
            eventType.getCode(),
            correspondence.getId(),
            messageKey,
            messageParams);
        notificationOutboxService.enqueueEmailIfPreferred(
            recipientId,
            eventType.getCode(),
            correspondence.getId(),
            messageKey,
            messageParams);
      }
      notificationOutboxService.enqueueIntegrationChannels(
          eventType.getCode(), correspondence.getId(), messageKey, messageParams);
      return;
    }
    for (UUID recipientId : recipientIds) {
      InAppNotificationEntity notification = new InAppNotificationEntity();
      notification.setRecipient(appUserRepository.getReferenceById(recipientId));
      notification.setEventType(eventType);
      notification.setCorrespondence(correspondenceRepository.getReferenceById(correspondence.getId()));
      notification.setMessageKey(messageKey);
      notification.setMessageParams(new HashMap<>(messageParams));
      notification.setReadAt(null);
      inAppNotificationRepository.save(notification);
    }
  }
}
