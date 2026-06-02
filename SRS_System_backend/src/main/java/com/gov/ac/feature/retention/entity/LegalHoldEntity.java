package com.gov.ac.feature.retention.entity;

import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
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
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "legal_hold", schema = "srs_system")
@Getter
@Setter
public class LegalHoldEntity extends SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @JoinColumn(name = "correspondence_id")
  private CorrespondenceEntity correspondence;

  @Column(name = "reason", nullable = false, columnDefinition = "text")
  private String reason;

  @Column(name = "placed_by", nullable = false)
  private UUID placedBy;

  @Column(name = "placed_at", nullable = false)
  private Instant placedAt;

  @Column(name = "released_at")
  private Instant releasedAt;

  @Column(name = "released_by")
  private UUID releasedBy;

  @Column(name = "release_reason", columnDefinition = "text")
  private String releaseReason;
}
