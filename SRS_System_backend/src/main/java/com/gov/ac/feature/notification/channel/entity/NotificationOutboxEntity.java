package com.gov.ac.feature.notification.channel.entity;

import com.gov.ac.feature.shared.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notification_outbox", schema = "srs_system")
@Getter
@Setter
public class NotificationOutboxEntity extends AuditableEntity {

  public static final String STATUS_PENDING = "PENDING";
  public static final String STATUS_IN_FLIGHT = "IN_FLIGHT";
  public static final String STATUS_SENT = "SENT";
  public static final String STATUS_FAILED = "FAILED";
  public static final String STATUS_DEAD = "DEAD";

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "idempotency_key", nullable = false, length = 128, unique = true)
  private String idempotencyKey;

  @Column(name = "event_type_code", nullable = false, length = 64)
  private String eventTypeCode;

  @Column(name = "channel_code", nullable = false, length = 32)
  private String channelCode;

  @Column(name = "recipient_user_id")
  private UUID recipientUserId;

  @Column(name = "recipient_address", length = 512)
  private String recipientAddress;

  @Column(name = "subject", length = 512)
  private String subject;

  @Column(name = "body_text", columnDefinition = "text")
  private String bodyText;

  @Column(name = "payload_json", columnDefinition = "text")
  private String payloadJson;

  @Column(name = "message_key", length = 128)
  private String messageKey;

  @Column(name = "message_params_json", columnDefinition = "text")
  private String messageParamsJson;

  @Column(name = "correlation_resource_type", length = 64)
  private String correlationResourceType;

  @Column(name = "correlation_resource_id", length = 64)
  private String correlationResourceId;

  @Column(name = "status", nullable = false, length = 16)
  private String status = STATUS_PENDING;

  @Column(name = "attempt_count", nullable = false)
  private Integer attemptCount = 0;

  @Column(name = "next_attempt_at", nullable = false)
  private Instant nextAttemptAt = Instant.now();

  @Column(name = "last_attempted_at")
  private Instant lastAttemptedAt;

  @Column(name = "last_error", columnDefinition = "text")
  private String lastError;
}
