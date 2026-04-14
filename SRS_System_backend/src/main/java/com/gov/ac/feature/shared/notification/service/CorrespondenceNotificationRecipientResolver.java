package com.gov.ac.feature.shared.notification.service;

import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
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
      CorrespondenceEntity correspondence, UUID excludeUserId) {
    Set<UUID> ids = new LinkedHashSet<>();
    if (correspondence.getOwnerDepartment() == null) {
      return ids;
    }
    Long deptId = correspondence.getOwnerDepartment().getId();
    for (AppUserEntity u : appUserRepository.findByDepartment_IdAndDeletedAtIsNullAndActiveTrue(deptId)) {
      if (excludeUserId == null || !u.getId().equals(excludeUserId)) {
        ids.add(u.getId());
      }
    }
    return ids;
  }
}
