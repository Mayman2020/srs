package com.gov.ac.modules.notification;

import com.gov.ac.domain.correspondence.Correspondence;
import com.gov.ac.domain.user.AppUser;
import com.gov.ac.persistence.AppUserRepository;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CorrespondenceNotificationRecipientResolver {

  private final AppUserRepository appUserRepository;

  /**
   * Active users in the correspondence owning department. If {@code excludeUserId} is non-null, that
   * user is omitted. Empty if there is no owner department.
   */
  public Set<UUID> ownerDepartmentRecipientsExcluding(
      Correspondence correspondence, UUID excludeUserId) {
    Set<UUID> ids = new LinkedHashSet<>();
    if (correspondence.getOwnerDepartment() == null) {
      return ids;
    }
    Long deptId = correspondence.getOwnerDepartment().getId();
    for (AppUser u : appUserRepository.findByDepartment_IdAndDeletedAtIsNullAndActiveTrue(deptId)) {
      if (excludeUserId == null || !u.getId().equals(excludeUserId)) {
        ids.add(u.getId());
      }
    }
    return ids;
  }
}
