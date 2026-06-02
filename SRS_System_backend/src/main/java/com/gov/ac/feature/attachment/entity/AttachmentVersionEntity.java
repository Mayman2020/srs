package com.gov.ac.feature.attachment.entity;

import com.gov.ac.feature.shared.entity.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "attachment_version", schema = "srs_system")
@Getter
@Setter
public class AttachmentVersionEntity extends SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "attachment_id", nullable = false)
  private AttachmentEntity attachment;

  @Column(name = "version_number", nullable = false)
  private Integer versionNumber;

  @Column(name = "storage_key", nullable = false, columnDefinition = "text")
  private String storageKey;

  @Column(name = "byte_size", nullable = false)
  private Long byteSize;

  @Column(name = "mime_type")
  private String mimeType;

  @Column(name = "checksum_sha256")
  private String checksumSha256;

  // ---------------------------------------------------------------------------
  // Slice 5 — at-rest encryption metadata. NULL on legacy (pre-V18) rows.
  // ---------------------------------------------------------------------------

  /** {@code AES_256_GCM} when the blob on disk is encrypted; NULL = legacy plaintext. */
  @Column(name = "encryption_algo")
  private String encryptionAlgo;

  /** KEK identifier used to wrap {@link #encryptionWrappedDek}. */
  @Column(name = "encryption_key_ref")
  private String encryptionKeyRef;

  /** DEK wrapped by the KEK (32-byte raw AES key, wrapped to 48 bytes for AES-GCM). */
  @Column(name = "encryption_wrapped_dek")
  private byte[] encryptionWrappedDek;

  /** 12-byte GCM nonce for the ciphertext. */
  @Column(name = "encryption_iv")
  private byte[] encryptionIv;

  /** SHA-256 of the on-disk ciphertext, hex-encoded (tamper-detection of storage). */
  @Column(name = "ciphertext_sha256")
  private String ciphertextSha256;

  /**
   * SHA-256 of the canonical plaintext, hex-encoded. Digital signatures bind to this value;
   * matches {@link #checksumSha256} for legacy rows when set.
   */
  @Column(name = "plaintext_sha256")
  private String plaintextSha256;
}
