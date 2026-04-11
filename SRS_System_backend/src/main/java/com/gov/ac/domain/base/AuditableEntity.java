package com.gov.ac.domain.base;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Standard audit columns: {@code created_at} / {@code updated_at} (timestamps) and {@code
 * created_by} / {@code updated_by} (UUID → {@code app_user.id}, FK in DB). Same intent as
 * CREATED_ON, MODIFIED_ON, CREATED_BY, MODIFIED_BY naming in other conventions.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditUserListener.class)
public abstract class AuditableEntity {

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "created_by")
  private UUID createdBy;

  @Column(name = "updated_by")
  private UUID updatedBy;
}
