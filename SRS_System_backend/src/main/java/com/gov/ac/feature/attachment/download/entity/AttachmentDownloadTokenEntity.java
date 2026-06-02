package com.gov.ac.feature.attachment.download.entity;

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

@Entity
@Table(name = "attachment_download_token", schema = "srs_system")
@Getter
@Setter
public class AttachmentDownloadTokenEntity extends AuditableEntity {

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

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "issued_at", nullable = false)
  private Instant issuedAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "consumed_at")
  private Instant consumedAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Column(name = "ip_address", length = 64)
  private String ipAddress;

  @Column(name = "user_agent", length = 512)
  private String userAgent;
}
