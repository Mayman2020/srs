package com.gov.ac.feature.correspondence.readtracking.entity;

import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.shared.entity.SoftDeletableEntity;
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
 * Persistent read receipt for a {@link CorrespondenceEntity} viewed by a single user.
 *
 * <p>A single row is maintained per (correspondence, user) for as long as it is not soft-deleted.
 * {@code openCount} / {@code lastOpenedAt} are bumped on every authorized detail view.
 * {@code acknowledgedAt} is filled once when the user explicitly acknowledges.
 */
@Entity
@Table(name = "correspondence_read_receipt", schema = "srs_system")
@Getter
@Setter
public class CorrespondenceReadReceiptEntity extends SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "correspondence_id", nullable = false)
  private CorrespondenceEntity correspondence;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUserEntity user;

  @Column(name = "first_opened_at", nullable = false)
  private Instant firstOpenedAt;

  @Column(name = "last_opened_at", nullable = false)
  private Instant lastOpenedAt;

  @Column(name = "open_count", nullable = false)
  private int openCount;

  @Column(name = "acknowledged_at")
  private Instant acknowledgedAt;

  @Column(name = "acknowledgement_comment", columnDefinition = "text")
  private String acknowledgementComment;
}
