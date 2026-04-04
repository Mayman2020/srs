package com.gov.ac.domain.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class SoftDeletableEntity extends AuditableEntity {

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @Column(name = "deleted_by")
  private UUID deletedBy;
}
