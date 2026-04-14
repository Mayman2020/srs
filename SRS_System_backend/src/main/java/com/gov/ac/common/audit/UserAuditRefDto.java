package com.gov.ac.common.audit;

import com.gov.ac.feature.users.entity.AppUserEntity;
import java.util.UUID;

/**
 * Resolved {@code app_user} for audit columns: UI picks {@link #fullNameAr()} or {@link
 * #fullNameEn()} from the active locale (same pattern as lookup labels).
 */
public record UserAuditRefDto(UUID id, String fullNameAr, String fullNameEn) {

  public static UserAuditRefDto from(AppUserEntity u) {
    return new UserAuditRefDto(u.getId(), u.getFullNameAr(), u.getFullNameEn());
  }
}
