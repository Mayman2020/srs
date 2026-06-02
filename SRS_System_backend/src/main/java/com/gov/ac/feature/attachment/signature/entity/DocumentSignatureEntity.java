package com.gov.ac.feature.attachment.signature.entity;

import com.gov.ac.feature.attachment.entity.AttachmentVersionEntity;
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
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "document_signature", schema = "srs_system")
@Getter
@Setter
public class DocumentSignatureEntity extends AuditableEntity {

  public static final String STATUS_VALID = "VALID";
  public static final String STATUS_REVOKED = "REVOKED";

  public static final String VERIFICATION_PENDING = "PENDING";
  public static final String VERIFICATION_VERIFIED = "VERIFIED";
  public static final String VERIFICATION_FAILED = "FAILED";

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "attachment_version_id", nullable = false)
  private AttachmentVersionEntity attachmentVersion;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "signer_user_id", nullable = false)
  private AppUserEntity signer;

  @Column(name = "algorithm", nullable = false, length = 32)
  private String algorithm;

  @Column(name = "canonical_hash_sha256", nullable = false, length = 64)
  private String canonicalHashSha256;

  @Column(name = "signature_bytes", nullable = false)
  private byte[] signatureBytes;

  @Column(name = "key_ref", nullable = false, length = 256)
  private String keyRef;

  @Column(name = "certificate_pem", columnDefinition = "text")
  private String certificatePem;

  @Column(name = "signed_at", nullable = false)
  private Instant signedAt;

  @Column(name = "status", nullable = false, length = 16)
  private String status = STATUS_VALID;

  @Column(name = "verification_status", nullable = false, length = 16)
  private String verificationStatus = VERIFICATION_PENDING;

  @Column(name = "verification_at")
  private Instant verificationAt;

  @Column(name = "verification_detail", columnDefinition = "text")
  private String verificationDetail;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Column(name = "revoked_by")
  private UUID revokedBy;
}
