package com.gov.ac.feature.leave.entity;

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
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "leave_request", schema = "srs_system")
@Getter
@Setter
public class LeaveRequestEntity extends SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUserEntity user;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Column(columnDefinition = "text")
  private String reason;

  @Column(name = "status_code", nullable = false, length = 64)
  private String statusCode = "PENDING";

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "decided_by")
  private AppUserEntity decidedBy;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "decision_note", columnDefinition = "text")
  private String decisionNote;
}
