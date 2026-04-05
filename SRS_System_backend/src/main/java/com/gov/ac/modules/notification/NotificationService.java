package com.gov.ac.modules.notification;

import com.gov.ac.domain.correspondence.Correspondence;
import com.gov.ac.domain.lookup.NotificationEventType;
import com.gov.ac.domain.notification.InAppNotification;
import com.gov.ac.domain.user.AppUser;
import com.gov.ac.modules.notification.dto.NotificationItemDto;
import com.gov.ac.persistence.AppUserRepository;
import com.gov.ac.persistence.CorrespondenceRepository;
import com.gov.ac.persistence.InAppNotificationRepository;
import com.gov.ac.persistence.NotificationEventTypeRepository;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.correspondence.audit.CorrespondenceActionAudit;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

  private final CorrespondenceActionAudit correspondenceActionAudit;
  private final InAppNotificationRepository inAppNotificationRepository;
  private final NotificationEventTypeRepository notificationEventTypeRepository;
  private final CorrespondenceNotificationRecipientResolver recipientResolver;
  private final CorrespondenceRepository correspondenceRepository;
  private final AppUserRepository appUserRepository;

  @Transactional(readOnly = true)
  public Page<NotificationItemDto> listInbox(UUID recipientId, Pageable pageable) {
    return inAppNotificationRepository
        .findByRecipient_IdAndDeletedAtIsNullOrderByCreatedAtDesc(recipientId, pageable)
        .map(this::toDto);
  }

  @Transactional
  public void deleteForRecipient(UUID notificationId, UUID recipientId) {
    InAppNotification n =
        inAppNotificationRepository
            .findByIdAndRecipient_IdAndDeletedAtIsNull(notificationId, recipientId)
            .orElseThrow(() -> new NotFoundException("Notification not found"));
    Instant now = Instant.now();
    n.setDeletedAt(now);
    n.setDeletedBy(recipientId);
    n.setUpdatedBy(recipientId);
    inAppNotificationRepository.save(n);
    correspondenceActionAudit.logResource(
        recipientId,
        CorrespondenceActionAudit.ACTION_NOTIFICATION_DELETE,
        "NOTIFICATION",
        notificationId.toString(),
        Map.of("eventType", n.getEventType().getCode()));
  }

  @Transactional
  public void markRead(UUID notificationId, UUID viewerId) {
    InAppNotification n =
        inAppNotificationRepository
            .findByIdAndRecipient_IdAndDeletedAtIsNull(notificationId, viewerId)
            .orElseThrow(() -> new NotFoundException("Notification not found"));
    if (n.getReadAt() == null) {
      n.setReadAt(Instant.now());
    }
  }

  @Transactional
  public void notifyCorrespondenceCreated(Correspondence correspondence, AppUser actor) {
    Optional<NotificationEventType> eventTypeOpt =
        notificationEventTypeRepository.findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(
            NotificationEventCodes.CORRESPONDENCE_CREATED);
    if (eventTypeOpt.isEmpty()) {
      log.error("Missing notification_event_type: {}", NotificationEventCodes.CORRESPONDENCE_CREATED);
      return;
    }
    Set<UUID> recipients =
        recipientResolver.ownerDepartmentRecipientsExcluding(correspondence, actor.getId());
    if (recipients.isEmpty()) {
      log.debug(
          "No inbox recipients for correspondence created (no owner dept or only actor): correspondenceId={}",
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
  public void notifyCommentAdded(Correspondence correspondence, AppUser author) {
    Optional<NotificationEventType> eventTypeOpt =
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
    Correspondence correspondence =
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
    String eventCode = mapTaskToNotificationEventCode(taskKey, taskName);

    Optional<NotificationEventType> eventTypeOpt =
        notificationEventTypeRepository.findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(eventCode);
    if (eventTypeOpt.isEmpty()) {
      log.error("Missing notification_event_type: {}", eventCode);
      return;
    }

    UUID completedBy = null;
    if (StringUtils.hasText(delegateTask.getAssignee())) {
      try {
        completedBy = UUID.fromString(delegateTask.getAssignee());
      } catch (IllegalArgumentException ignored) {
        // assignee may not be a UUID in some process definitions
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

  static String mapTaskToNotificationEventCode(String taskDefinitionKey, String taskName) {
    String blob = (taskName + " " + taskDefinitionKey).toLowerCase();
    if (blob.contains("approve")) {
      return NotificationEventCodes.APPROVED;
    }
    if (blob.contains("reject")) {
      return NotificationEventCodes.REJECTED;
    }
    if (blob.contains("return")) {
      return NotificationEventCodes.RETURNED;
    }
    return NotificationEventCodes.ASSIGNED;
  }

  private NotificationItemDto toDto(InAppNotification n) {
    return NotificationItemDto.builder()
        .id(n.getId())
        .userId(n.getRecipient().getId())
        .type(n.getEventType().getCode())
        .messageKey(n.getMessageKey())
        .messageParams(
            n.getMessageParams() != null ? Map.copyOf(n.getMessageParams()) : Map.of())
        .read(n.getReadAt() != null)
        .createdAt(n.getCreatedAt())
        .build();
  }

  private Map<String, Object> baseCorrespondenceParams(Correspondence correspondence) {
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
      NotificationEventType eventType,
      Correspondence correspondence,
      String messageKey,
      Map<String, Object> messageParams) {
    for (UUID recipientId : recipientIds) {
      InAppNotification n = new InAppNotification();
      n.setRecipient(appUserRepository.getReferenceById(recipientId));
      n.setEventType(eventType);
      n.setCorrespondence(correspondenceRepository.getReferenceById(correspondence.getId()));
      n.setMessageKey(messageKey);
      n.setMessageParams(new HashMap<>(messageParams));
      n.setReadAt(null);
      inAppNotificationRepository.save(n);
    }
  }
}
