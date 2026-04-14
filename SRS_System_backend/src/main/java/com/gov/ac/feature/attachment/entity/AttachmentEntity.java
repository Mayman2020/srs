package com.gov.ac.feature.attachment.entity;

import com.gov.ac.feature.shared.entity.SoftDeletableEntity;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.lookups.entity.AttachmentContentTypeEntity;
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
@Table(name = "attachment", schema = "srs_system")
@Getter
@Setter
public class AttachmentEntity extends SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "correspondence_id", nullable = false)
  private CorrespondenceEntity correspondence;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "content_type_id")
  private AttachmentContentTypeEntity contentType;

  @Column(name = "display_name", nullable = false, length = 500)
  private String displayName;

  @Column(name = "current_version_id")
  private Long currentVersionId;

  @Column(name = "is_active", nullable = false)
  private Boolean active = true;
}
