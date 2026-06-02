package com.gov.ac.feature.notification.inbox.service;

import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.feature.notification.inbox.entity.InAppNotificationEntity;
import com.gov.ac.feature.correspondence.audit.CorrespondenceActionAudit;
import com.gov.ac.feature.notification.inbox.dto.NotificationItemDto;
import com.gov.ac.feature.notification.inbox.mapper.NotificationInboxMapper;
import com.gov.ac.feature.notification.inbox.repository.InAppNotificationRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationInboxService {

  private final CorrespondenceActionAudit correspondenceActionAudit;
  private final InAppNotificationRepository inAppNotificationRepository;

  @Transactional(readOnly = true)
  public Page<NotificationItemDto> listInbox(UUID recipientId, Pageable pageable) {
    return inAppNotificationRepository
        .findByRecipient_IdAndDeletedAtIsNull(recipientId, pageable)
        .map(NotificationInboxMapper::toDto);
  }

  @Transactional
  public void deleteForRecipient(UUID notificationId, UUID recipientId) {
    InAppNotificationEntity notification =
        inAppNotificationRepository
            .findByIdAndRecipient_IdAndDeletedAtIsNull(notificationId, recipientId)
            .orElseThrow(() -> new NotFoundException("Notification not found"));
    Instant now = Instant.now();
    notification.setDeletedAt(now);
    notification.setDeletedBy(recipientId);
    notification.setUpdatedBy(recipientId);
    inAppNotificationRepository.save(notification);
    correspondenceActionAudit.logResource(
        recipientId,
        CorrespondenceActionAudit.ACTION_NOTIFICATION_DELETE,
        "NOTIFICATION",
        notificationId.toString(),
        Map.of("eventType", notification.getEventType().getCode()));
  }

  @Transactional
  public void markRead(UUID notificationId, UUID viewerId) {
    InAppNotificationEntity notification =
        inAppNotificationRepository
            .findByIdAndRecipient_IdAndDeletedAtIsNull(notificationId, viewerId)
            .orElseThrow(() -> new NotFoundException("Notification not found"));
    if (notification.getReadAt() == null) {
      notification.setReadAt(Instant.now());
    }
  }

  /**
   * Bulk-marks every unread, non-deleted notification for the given recipient as read.
   * Executes a single UPDATE rather than N individual saves.
   *
   * @return number of rows updated
   */
  @Transactional
  public int markAllRead(UUID recipientId) {
    return inAppNotificationRepository.markAllReadForRecipient(recipientId, Instant.now());
  }
}
