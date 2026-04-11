package com.gov.ac.common.audit;

import com.gov.ac.domain.user.AppUser;
import java.util.UUID;

/**
 * Resolved {@code app_user} for audit columns: UI picks {@link #fullNameAr()} or {@link
 * #fullNameEn()} from the active locale (same pattern as lookup labels).
 */
public record UserAuditRefDto(UUID id, String fullNameAr, String fullNameEn) {

  public static UserAuditRefDto from(AppUser u) {
    return new UserAuditRefDto(u.getId(), u.getFullNameAr(), u.getFullNameEn());
  }
}
