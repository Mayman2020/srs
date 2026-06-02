package com.gov.ac.feature.sla.notification;

import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.lookups.entity.NotificationEventTypeEntity;
import com.gov.ac.feature.lookups.repository.NotificationEventTypeRepository;
import com.gov.ac.feature.notification.channel.NotificationOutboxService;
import com.gov.ac.feature.notification.channel.NotificationRoutingProperties;
import com.gov.ac.feature.notification.inbox.entity.InAppNotificationEntity;
import com.gov.ac.feature.notification.inbox.repository.InAppNotificationRepository;
import com.gov.ac.feature.users.repository.AppUserRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Minimal in-app notification publisher for the SLA Policy Engine.
 *
 * <p>Coexists with the existing {@link com.gov.ac.feature.shared.notification.service.NotificationService}
 * (which is correspondence-event oriented and exposes only domain methods like {@code
 * notifyCorrespondenceCreated}). The SLA flow does not fit any existing public method on that
 * service, so we have a tiny isolated helper here rather than extending the established
 * notification API surface. Both writers persist to the same {@code notification} table, so the
 * inbox UI sees them identically.
 *
 * <p>Writes run in {@link Propagation#REQUIRES_NEW} so a failure here cannot roll back the
 * escalation step state update — operational continuity over best-effort reach.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SlaNotifier {

  /** Reuses the V1-seeded {@code OVERDUE} event type when no explicit code is supplied. */
  public static final String DEFAULT_EVENT_CODE = "OVERDUE";

  /** i18n key resolved by the Angular shell. */
  public static final String MESSAGE_KEY_SLA_BREACH = "notification.sla.breach";

  private final NotificationEventTypeRepository notificationEventTypeRepository;
  private final InAppNotificationRepository inAppNotificationRepository;
  private final AppUserRepository appUserRepository;
  private final NotificationOutboxService notificationOutboxService;
  private final NotificationRoutingProperties notificationRoutingProperties;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public int notifyRecipients(
      Collection<UUID> recipientIds,
      String eventCode,
      CorrespondenceEntity correspondence,
      String messageKey,
      Map<String, Object> messageParams) {
    if (recipientIds == null || recipientIds.isEmpty()) {
      return 0;
    }
    String resolvedCode = (eventCode == null || eventCode.isBlank()) ? DEFAULT_EVENT_CODE : eventCode;
    Optional<NotificationEventTypeEntity> eventTypeOpt =
        notificationEventTypeRepository.findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(resolvedCode);
    if (eventTypeOpt.isEmpty()) {
      log.warn(
          "[SLA] notification skipped: missing notification_event_type code={}; "
              + "ensure V1 seed (DUE_SOON/OVERDUE) is present.",
          resolvedCode);
      return 0;
    }
    NotificationEventTypeEntity eventType = eventTypeOpt.get();
    int written = 0;
    for (UUID recipientId : recipientIds) {
      if (recipientId == null) {
        continue;
      }
      try {
        if (notificationRoutingProperties.isOutbox()) {
          notificationOutboxService.enqueueInApp(
              recipientId,
              eventType.getCode(),
              correspondence != null ? correspondence.getId() : null,
              messageKey != null ? messageKey : MESSAGE_KEY_SLA_BREACH,
              messageParams == null ? new HashMap<>() : new HashMap<>(messageParams));
          notificationOutboxService.enqueueEmailIfPreferred(
              recipientId,
              eventType.getCode(),
              correspondence != null ? correspondence.getId() : null,
              messageKey != null ? messageKey : MESSAGE_KEY_SLA_BREACH,
              messageParams == null ? new HashMap<>() : new HashMap<>(messageParams));
          written++;
          continue;
        }
        InAppNotificationEntity n = new InAppNotificationEntity();
        n.setRecipient(appUserRepository.getReferenceById(recipientId));
        n.setEventType(eventType);
        if (correspondence != null) {
          n.setCorrespondence(correspondence);
        }
        n.setMessageKey(messageKey != null ? messageKey : MESSAGE_KEY_SLA_BREACH);
        n.setMessageParams(messageParams == null ? new HashMap<>() : new HashMap<>(messageParams));
        n.setReadAt(null);
        inAppNotificationRepository.save(n);
        written++;
      } catch (RuntimeException ex) {
        log.warn(
            "[SLA] notification persistence failed for recipient={} : {}",
            recipientId,
            ex.getMessage());
      }
    }
    if (notificationRoutingProperties.isOutbox() && written > 0) {
      notificationOutboxService.enqueueIntegrationChannels(
          eventType.getCode(),
          correspondence != null ? correspondence.getId() : null,
          messageKey != null ? messageKey : MESSAGE_KEY_SLA_BREACH,
          messageParams == null ? new HashMap<>() : new HashMap<>(messageParams));
    }
    return written;
  }
}
