package com.gov.ac.domain.correspondence;

import com.gov.ac.domain.base.SoftDeletableEntity;
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
@Table(name = "attachment_version")
@Getter
@Setter
public class AttachmentVersion extends SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "attachment_id", nullable = false)
  private Attachment attachment;

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
}
