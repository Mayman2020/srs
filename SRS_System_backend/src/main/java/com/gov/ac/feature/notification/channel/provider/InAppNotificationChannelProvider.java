package com.gov.ac.feature.notification.channel.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.feature.lookups.repository.NotificationEventTypeRepository;
import com.gov.ac.feature.notification.channel.NotificationOutboxService;
import com.gov.ac.feature.notification.channel.entity.NotificationOutboxEntity;
import com.gov.ac.feature.notification.inbox.entity.InAppNotificationEntity;
import com.gov.ac.feature.notification.inbox.repository.InAppNotificationRepository;
import com.gov.ac.feature.users.repository.AppUserRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InAppNotificationChannelProvider implements NotificationChannelProvider {

  private final InAppNotificationRepository inAppNotificationRepository;
  private final NotificationEventTypeRepository notificationEventTypeRepository;
  private final CorrespondenceRepository correspondenceRepository;
  private final AppUserRepository appUserRepository;
  private final ObjectMapper objectMapper;

  @Override
  public String code() {
    return NotificationOutboxService.CHANNEL_IN_APP;
  }

  @Override
  public boolean supports(NotificationOutboxEntity row) {
    return NotificationOutboxService.CHANNEL_IN_APP.equalsIgnoreCase(row.getChannelCode());
  }

  @Override
  public void dispatch(NotificationOutboxEntity row) throws Exception {
    var eventType =
        notificationEventTypeRepository
            .findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(row.getEventTypeCode())
            .orElseThrow(() -> new IllegalStateException("Unknown event type: " + row.getEventTypeCode()));
    UUID recipientId = row.getRecipientUserId();
    InAppNotificationEntity n = new InAppNotificationEntity();
    n.setRecipient(appUserRepository.getReferenceById(recipientId));
    n.setEventType(eventType);
    if (row.getCorrelationResourceType() != null
        && "CORRESPONDENCE".equalsIgnoreCase(row.getCorrelationResourceType())
        && row.getCorrelationResourceId() != null) {
      UUID cid = UUID.fromString(row.getCorrelationResourceId());
      n.setCorrespondence(correspondenceRepository.getReferenceById(cid));
    }
    n.setMessageKey(row.getMessageKey());
    Map<String, Object> params = new HashMap<>();
    if (row.getMessageParamsJson() != null && !row.getMessageParamsJson().isBlank()) {
      params.putAll(
          objectMapper.readValue(row.getMessageParamsJson(), new TypeReference<Map<String, Object>>() {}));
    }
    n.setMessageParams(params);
    n.setReadAt(null);
    inAppNotificationRepository.save(n);
  }
}
