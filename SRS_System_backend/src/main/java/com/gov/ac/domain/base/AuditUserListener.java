package com.gov.ac.domain.base;

import com.gov.ac.security.SecurityUtils;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.util.UUID;

/**
 * Fills {@link AuditableEntity#getCreatedBy()}, {@link AuditableEntity#getUpdatedBy()} from the
 * security principal (JWT {@code sub} = {@code app_user.id}). Timestamps use {@code created_at} /
 * {@code updated_at} columns (DB equivalent of CREATED_ON / MODIFIED_ON).
 */
public class AuditUserListener {

  @PrePersist
  public void onCreate(Object entity) {
    if (!(entity instanceof AuditableEntity a)) {
      return;
    }
    UUID uid = SecurityUtils.currentUserId().orElse(null);
    if (a.getCreatedBy() == null) {
      a.setCreatedBy(uid);
    }
    if (a.getUpdatedBy() == null) {
      a.setUpdatedBy(uid);
    }
  }

  @PreUpdate
  public void onUpdate(Object entity) {
    if (!(entity instanceof AuditableEntity a)) {
      return;
    }
    SecurityUtils.currentUserId().ifPresent(a::setUpdatedBy);
  }
}
