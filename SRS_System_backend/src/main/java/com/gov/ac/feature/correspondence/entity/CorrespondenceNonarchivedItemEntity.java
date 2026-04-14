package com.gov.ac.feature.correspondence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "correspondence_nonarchived_item", schema = "srs_system")
@Getter
@Setter
public class CorrespondenceNonarchivedItemEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "correspondence_id", nullable = false)
  private CorrespondenceEntity correspondence;

  @Column(name = "item_type", nullable = false, length = 128)
  private String itemType;

  @Column(name = "description_text", columnDefinition = "text")
  private String descriptionText;

  @Column(nullable = false)
  private Integer quantity = 1;

  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder = 0;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "created_by")
  private UUID createdBy;

  @PrePersist
  void prePersist() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
    if (quantity == null) {
      quantity = 1;
    }
    if (sortOrder == null) {
      sortOrder = 0;
    }
  }
}
