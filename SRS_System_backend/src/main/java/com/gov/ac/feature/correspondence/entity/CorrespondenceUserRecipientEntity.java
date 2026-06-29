package com.gov.ac.feature.correspondence.entity;

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

@Entity
@Table(name = "correspondence_user_recipient", schema = "srs_system")
@Getter
@Setter
public class CorrespondenceUserRecipientEntity extends SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "correspondence_id", nullable = false)
  private CorrespondenceEntity correspondence;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "recipient_user_id", nullable = false)
  private AppUserEntity recipientUser;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "recipient_kind_id", nullable = false)
  private CorrespondenceRecipientKindEntity recipientKind;

  @Column(name = "first_read_at")
  private Instant firstReadAt;

  @Column(name = "last_read_at")
  private Instant lastReadAt;

  @Column(name = "read_count", nullable = false)
  private int readCount = 0;

  @Column(name = "acknowledged_at")
  private Instant acknowledgedAt;

  @Column(columnDefinition = "text")
  private String notes;
}
