package com.gov.ac.feature.notification.channel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gov.ac.feature.notification.channel.entity.NotificationChannelTargetEntity;
import com.gov.ac.feature.notification.channel.entity.NotificationOutboxEntity;
import com.gov.ac.feature.notification.channel.repository.NotificationChannelTargetRepository;
import com.gov.ac.feature.notification.channel.repository.NotificationOutboxRepository;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationOutboxService {

  public static final String CHANNEL_IN_APP = "IN_APP";
  public static final String CHANNEL_EMAIL = "EMAIL";
  public static final String CHANNEL_WEBHOOK = "WEBHOOK";
  public static final String CHANNEL_TEAMS = "TEAMS";

  private final NotificationOutboxRepository outboxRepository;
  private final NotificationPreferenceService preferenceService;
  private final NotificationChannelTargetRepository channelTargetRepository;
  private final NotificationTeamsProperties teamsProperties;
  private final AppUserRepository appUserRepository;
  private final ObjectMapper objectMapper;

  @Transactional
  public void enqueueInApp(
      UUID recipientUserId,
      String eventTypeCode,
      UUID correspondenceId,
      String messageKey,
      Map<String, Object> messageParams) {
    if (recipientUserId == null || eventTypeCode == null) {
      return;
    }
    if (!preferenceService.isEnabled(recipientUserId, eventTypeCode, CHANNEL_IN_APP)) {
      return;
    }
    String idempotencyKey =
        eventTypeCode
            + ":IN_APP:"
            + recipientUserId
            + ":"
            + (correspondenceId != null ? correspondenceId : "-")
            + ":"
            + messageKey;
    saveOutboxRow(
        idempotencyKey,
        eventTypeCode,
        CHANNEL_IN_APP,
        recipientUserId,
        null,
        null,
        null,
        null,
        messageKey,
        messageParams,
        correspondenceId);
  }

  @Transactional
  public void enqueueEmailIfPreferred(
      UUID recipientUserId,
      String eventTypeCode,
      UUID correspondenceId,
      String messageKey,
      Map<String, Object> messageParams) {
    if (recipientUserId == null || eventTypeCode == null) {
      return;
    }
    if (!preferenceService.isEnabled(recipientUserId, eventTypeCode, CHANNEL_EMAIL)) {
      return;
    }
    AppUserEntity user = appUserRepository.findById(recipientUserId).orElse(null);
    if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
      return;
    }
    String idempotencyKey =
        eventTypeCode
            + ":EMAIL:"
            + recipientUserId
            + ":"
            + (correspondenceId != null ? correspondenceId : "-")
            + ":"
            + messageKey;
    String subject = messageKey != null ? messageKey : "Notification";
    String body = formatEmailBody(messageKey, messageParams);
    saveOutboxRow(
        idempotencyKey,
        eventTypeCode,
        CHANNEL_EMAIL,
        recipientUserId,
        user.getEmail(),
        subject,
        body,
        null,
        messageKey,
        messageParams,
        correspondenceId);
  }

  /**
   * Enqueues one durable row per configured webhook / Teams target for integration fan-out.
   * Idempotent per (event, correspondence, messageKey, target).
   */
  @Transactional
  public void enqueueIntegrationChannels(
      String eventTypeCode,
      UUID correspondenceId,
      String messageKey,
      Map<String, Object> messageParams) {
    if (eventTypeCode == null) {
      return;
    }
    Map<String, Object> notification =
        new LinkedHashMap<>(
            Map.of(
                "eventTypeCode",
                eventTypeCode,
                "messageKey",
                messageKey != null ? messageKey : "",
                "correspondenceId",
                correspondenceId != null ? correspondenceId.toString() : "",
                "params",
                messageParams != null ? messageParams : Map.of()));

    List<NotificationChannelTargetEntity> webhooks =
        channelTargetRepository.findByChannelCodeAndEnabledTrueAndDeletedAtIsNull(CHANNEL_WEBHOOK);
    for (NotificationChannelTargetEntity t : webhooks) {
      enqueueTargetRow(CHANNEL_WEBHOOK, t, eventTypeCode, correspondenceId, messageKey, notification);
    }

    List<NotificationChannelTargetEntity> teamsTargets =
        channelTargetRepository.findByChannelCodeAndEnabledTrueAndDeletedAtIsNull(CHANNEL_TEAMS);
    String onlyCode = teamsProperties.defaultTargetCode();
    for (NotificationChannelTargetEntity t : teamsTargets) {
      if (!onlyCode.isEmpty() && !onlyCode.equalsIgnoreCase(t.getTargetCode())) {
        continue;
      }
      enqueueTargetRow(CHANNEL_TEAMS, t, eventTypeCode, correspondenceId, messageKey, notification);
    }
  }

  private void enqueueTargetRow(
      String channelCode,
      NotificationChannelTargetEntity target,
      String eventTypeCode,
      UUID correspondenceId,
      String messageKey,
      Map<String, Object> notification) {
    Map<String, Object> envelope = new LinkedHashMap<>();
    envelope.put("targetCode", target.getTargetCode());
    envelope.put("notification", notification);
    String idempotencyKey =
        eventTypeCode
            + ":"
            + channelCode
            + ":"
            + target.getTargetCode()
            + ":"
            + (correspondenceId != null ? correspondenceId : "-")
            + ":"
            + (messageKey != null ? messageKey : "");
    String payloadJson;
    try {
      payloadJson = objectMapper.writeValueAsString(envelope);
    } catch (JsonProcessingException e) {
      payloadJson = "{}";
    }
    NotificationOutboxEntity row = new NotificationOutboxEntity();
    row.setIdempotencyKey(idempotencyKey);
    row.setEventTypeCode(eventTypeCode);
    row.setChannelCode(channelCode);
    row.setRecipientUserId(null);
    row.setRecipientAddress(null);
    row.setSubject(null);
    row.setBodyText(null);
    row.setPayloadJson(payloadJson);
    row.setMessageKey(messageKey);
    try {
      row.setMessageParamsJson(objectMapper.writeValueAsString(notification));
    } catch (JsonProcessingException e) {
      row.setMessageParamsJson("{}");
    }
    if (correspondenceId != null) {
      row.setCorrelationResourceType("CORRESPONDENCE");
      row.setCorrelationResourceId(correspondenceId.toString());
    }
    row.setStatus(NotificationOutboxEntity.STATUS_PENDING);
    row.setAttemptCount(0);
    row.setNextAttemptAt(Instant.now());
    try {
      outboxRepository.save(row);
    } catch (DataIntegrityViolationException dup) {
      log.debug("notification_outbox dedup hit key={}", idempotencyKey);
    }
  }

  private void saveOutboxRow(
      String idempotencyKey,
      String eventTypeCode,
      String channelCode,
      UUID recipientUserId,
      String recipientAddress,
      String subject,
      String bodyText,
      String payloadJson,
      String messageKey,
      Map<String, Object> messageParams,
      UUID correspondenceId) {
    NotificationOutboxEntity row = new NotificationOutboxEntity();
    row.setIdempotencyKey(idempotencyKey);
    row.setEventTypeCode(eventTypeCode);
    row.setChannelCode(channelCode);
    row.setRecipientUserId(recipientUserId);
    row.setRecipientAddress(recipientAddress);
    row.setSubject(subject);
    row.setBodyText(bodyText);
    row.setPayloadJson(payloadJson);
    row.setMessageKey(messageKey);
    try {
      row.setMessageParamsJson(
          objectMapper.writeValueAsString(messageParams != null ? messageParams : Map.of()));
    } catch (JsonProcessingException e) {
      row.setMessageParamsJson("{}");
    }
    if (correspondenceId != null) {
      row.setCorrelationResourceType("CORRESPONDENCE");
      row.setCorrelationResourceId(correspondenceId.toString());
    }
    row.setStatus(NotificationOutboxEntity.STATUS_PENDING);
    row.setAttemptCount(0);
    row.setNextAttemptAt(Instant.now());
    try {
      outboxRepository.save(row);
    } catch (DataIntegrityViolationException dup) {
      log.debug("notification_outbox dedup hit key={}", idempotencyKey);
    }
  }

  private static String formatEmailBody(String messageKey, Map<String, Object> messageParams) {
    StringBuilder sb = new StringBuilder();
    if (messageKey != null) {
      sb.append(messageKey).append("\n\n");
    }
    if (messageParams != null) {
      for (Map.Entry<String, Object> e : messageParams.entrySet()) {
        sb.append(e.getKey()).append(": ").append(e.getValue()).append('\n');
      }
    }
    return sb.toString();
  }
}
