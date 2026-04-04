package com.gov.ac.domain.correspondence;

import com.gov.ac.domain.base.SoftDeletableEntity;
import com.gov.ac.domain.lookup.AttachmentContentType;
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
@Table(name = "attachment")
@Getter
@Setter
public class Attachment extends SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "correspondence_id", nullable = false)
  private Correspondence correspondence;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "content_type_id")
  private AttachmentContentType contentType;

  @Column(name = "display_name", nullable = false, length = 500)
  private String displayName;

  @Column(name = "current_version_id")
  private Long currentVersionId;

  @Column(name = "is_active", nullable = false)
  private Boolean active = true;
}
