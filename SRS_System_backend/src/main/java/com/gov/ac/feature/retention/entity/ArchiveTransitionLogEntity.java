package com.gov.ac.feature.retention.entity;

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
@Table(name = "archive_transition_log", schema = "srs_system")
@Getter
@Setter
public class ArchiveTransitionLogEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "applied_to", nullable = false, length = 32)
  private String appliedTo;

  @Column(name = "resource_id", nullable = false, length = 64)
  private String resourceId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "policy_id")
  private RetentionPolicyEntity policy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "legal_hold_id")
  private LegalHoldEntity legalHold;

  @Column(name = "action", nullable = false, length = 24)
  private String action;

  @Column(name = "executed_at", nullable = false)
  private Instant executedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "executed_by")
  private AppUserEntity executedBy;

  @Column(name = "detail_json", columnDefinition = "text")
  private String detailJson;
}
