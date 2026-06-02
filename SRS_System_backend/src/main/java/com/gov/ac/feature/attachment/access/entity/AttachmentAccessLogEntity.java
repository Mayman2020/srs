package com.gov.ac.feature.attachment.access.entity;

import com.gov.ac.feature.attachment.entity.AttachmentEntity;
import com.gov.ac.feature.attachment.entity.AttachmentVersionEntity;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.shared.entity.AuditableEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;
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
import lombok.Getter;
import lombok.Setter;

/**
 * Append-only access trail for attachment downloads / metadata views. Extends {@link
 * AuditableEntity} (not {@link com.gov.ac.feature.shared.entity.SoftDeletableEntity}) — rows are
 * never deleted in normal operation.
 */
@Entity
@Table(name = "attachment_access_log", schema = "srs_system")
@Getter
@Setter
public class AttachmentAccessLogEntity extends AuditableEntity {

  public static final String ACTION_DOWNLOAD = "DOWNLOAD";
  public static final String ACTION_VIEW_METADATA = "VIEW_METADATA";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "attachment_version_id", nullable = false)
  private AttachmentVersionEntity attachmentVersion;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "attachment_id", nullable = false)
  private AttachmentEntity attachment;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "correspondence_id", nullable = false)
  private CorrespondenceEntity correspondence;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUserEntity user;

  @Column(name = "action_code", nullable = false, length = 64)
  private String actionCode;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "ip_address", length = 64)
  private String ipAddress;

  @Column(name = "user_agent", length = 512)
  private String userAgent;

  @Column(name = "success", nullable = false)
  private boolean success;
}
