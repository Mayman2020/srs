package com.gov.ac.feature.attachment.verification.entity;

import com.gov.ac.feature.attachment.entity.AttachmentEntity;
import com.gov.ac.feature.attachment.entity.AttachmentVersionEntity;
import com.gov.ac.feature.shared.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Long-lived (but revocable) opaque token embedded in a printed QR code. Public scans hit the
 * permitAll verify endpoint and the row's {@code access_count} / {@code last_accessed_at} are
 * updated; failures are logged separately in {@link AttachmentVerificationAccessLogEntity}.
 */
@Entity
@Table(name = "attachment_verification_token", schema = "srs_system")
@Getter
@Setter
public class AttachmentVerificationTokenEntity extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "token_hash", nullable = false, length = 64)
  private String tokenHash;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "attachment_id", nullable = false)
  private AttachmentEntity attachment;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "attachment_version_id", nullable = false)
  private AttachmentVersionEntity attachmentVersion;

  @Column(name = "issued_by", nullable = false)
  private UUID issuedBy;

  @Column(name = "issued_at", nullable = false)
  private Instant issuedAt;

  @Column(name = "expires_at")
  private Instant expiresAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Column(name = "revoked_by")
  private UUID revokedBy;

  @Column(name = "access_count", nullable = false)
  private Integer accessCount = 0;

  @Column(name = "last_accessed_at")
  private Instant lastAccessedAt;
}
