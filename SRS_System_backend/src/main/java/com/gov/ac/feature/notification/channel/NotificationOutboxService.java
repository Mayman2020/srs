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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
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
  public static final String CHANNEL_SMS = "SMS";
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
    enqueueInApp(recipientUserId, eventTypeCode, correspondenceId, messageKey, messageParams, null);
  }

  @Transactional
  public void enqueueInApp(
      UUID recipientUserId,
      String eventTypeCode,
      UUID correspondenceId,
      String messageKey,
      Map<String, Object> messageParams,
      String occurrenceKey) {
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
            + messageKey
            + occurrenceSuffix(occurrenceKey);
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
    enqueueEmailIfPreferred(
        recipientUserId, eventTypeCode, correspondenceId, messageKey, messageParams, null);
  }

  @Transactional
  public void enqueueEmailIfPreferred(
      UUID recipientUserId,
      String eventTypeCode,
      UUID correspondenceId,
      String messageKey,
      Map<String, Object> messageParams,
      String occurrenceKey) {
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
            + messageKey
            + occurrenceSuffix(occurrenceKey);
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
   * Direct delivery for auth / security flows — bypasses user notification preferences.
   */
  @Transactional
  public void enqueueDirectEmail(UUID userId, String to, String eventTypeCode, String subject, String body) {
    if (to == null || to.isBlank()) {
      return;
    }
    String idempotencyKey = eventTypeCode + ":EMAIL:DIRECT:" + userId + ":" + Instant.now().toEpochMilli();
    saveOutboxRow(
        idempotencyKey,
        eventTypeCode,
        CHANNEL_EMAIL,
        userId,
        to.trim(),
        subject,
        body,
        null,
        subject,
        Map.of("subject", subject != null ? subject : ""),
        null);
  }

  /** Direct SMS delivery for auth / security flows — bypasses user notification preferences. */
  @Transactional
  public void enqueueDirectSms(UUID userId, String phoneE164, String eventTypeCode, String body) {
    if (phoneE164 == null || phoneE164.isBlank()) {
      return;
    }
    String idempotencyKey = eventTypeCode + ":SMS:DIRECT:" + userId + ":" + Instant.now().toEpochMilli();
    saveOutboxRow(
        idempotencyKey,
        eventTypeCode,
        CHANNEL_SMS,
        userId,
        phoneE164.trim(),
        null,
        body,
        null,
        eventTypeCode,
        Map.of("message", body != null ? body : ""),
        null);
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
    enqueueIntegrationChannels(eventTypeCode, correspondenceId, messageKey, messageParams, null);
  }

  @Transactional
  public void enqueueIntegrationChannels(
      String eventTypeCode,
      UUID correspondenceId,
      String messageKey,
      Map<String, Object> messageParams,
      String occurrenceKey) {
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
      enqueueTargetRow(
          CHANNEL_WEBHOOK, t, eventTypeCode, correspondenceId, messageKey, notification, occurrenceKey);
    }

    List<NotificationChannelTargetEntity> teamsTargets =
        channelTargetRepository.findByChannelCodeAndEnabledTrueAndDeletedAtIsNull(CHANNEL_TEAMS);
    String onlyCode = teamsProperties.defaultTargetCode();
    for (NotificationChannelTargetEntity t : teamsTargets) {
      if (!onlyCode.isEmpty() && !onlyCode.equalsIgnoreCase(t.getTargetCode())) {
        continue;
      }
      enqueueTargetRow(
          CHANNEL_TEAMS, t, eventTypeCode, correspondenceId, messageKey, notification, occurrenceKey);
    }
  }

  private void enqueueTargetRow(
      String channelCode,
      NotificationChannelTargetEntity target,
      String eventTypeCode,
      UUID correspondenceId,
      String messageKey,
      Map<String, Object> notification,
      String occurrenceKey) {
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
            + (messageKey != null ? messageKey : "")
            + occurrenceSuffix(occurrenceKey);
    String payloadJson;
    try {
      payloadJson = objectMapper.writeValueAsString(envelope);
    } catch (JsonProcessingException e) {
      payloadJson = "{}";
    }
    NotificationOutboxEntity row = new NotificationOutboxEntity();
    row.setIdempotencyKey(compactIdempotencyKey(idempotencyKey));
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
    row.setIdempotencyKey(compactIdempotencyKey(idempotencyKey));
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

  private static String occurrenceSuffix(String occurrenceKey) {
    return occurrenceKey == null || occurrenceKey.isBlank() ? "" : ":" + occurrenceKey.trim();
  }

  private static String compactIdempotencyKey(String key) {
    if (key.length() <= 128) {
      return key;
    }
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
      return "sha256:" + HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is unavailable", ex);
    }
  }
}
