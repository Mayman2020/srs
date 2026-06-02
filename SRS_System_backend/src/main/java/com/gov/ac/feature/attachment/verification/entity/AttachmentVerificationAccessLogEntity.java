package com.gov.ac.feature.attachment.verification.entity;

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

/**
 * Forensic trail for every public verification scan. Distinct from {@code audit_event} which
 * stays human-actor-only — here the actor is anonymous (IP + user-agent + the SHA-256 of the
 * presented token).
 */
@Entity
@Table(name = "attachment_verification_access_log", schema = "srs_system")
@Getter
@Setter
public class AttachmentVerificationAccessLogEntity {

  public static final String REASON_OK = "OK";
  public static final String REASON_UNKNOWN = "UNKNOWN_TOKEN";
  public static final String REASON_REVOKED = "REVOKED";
  public static final String REASON_EXPIRED = "EXPIRED";
  public static final String REASON_RATE_LIMITED = "RATE_LIMITED";

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "token_hash", nullable = false, length = 64)
  private String tokenHash;

  @Column(name = "attachment_version_id")
  private Long attachmentVersionId;

  @Column(name = "accessed_at", nullable = false)
  private Instant accessedAt;

  @Column(name = "ip_address", length = 64)
  private String ipAddress;

  @Column(name = "user_agent", length = 512)
  private String userAgent;

  @Column(name = "success", nullable = false)
  private Boolean success;

  @Column(name = "failure_reason", length = 64)
  private String failureReason;
}
