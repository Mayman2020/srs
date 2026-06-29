package com.gov.ac.feature.correspondence.entity;

import com.gov.ac.feature.departments.entity.DepartmentEntity;
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
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "correspondence_recipient", schema = "srs_system")
@Getter
@Setter
public class CorrespondenceRecipientEntity extends SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "correspondence_id", nullable = false)
  private CorrespondenceEntity correspondence;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "department_id", nullable = false)
  private DepartmentEntity department;

  @Column(name = "first_read_at")
  private Instant firstReadAt;

  @Column(name = "last_read_at")
  private Instant lastReadAt;

  @Column(name = "read_count", nullable = false)
  private int readCount = 0;
}
