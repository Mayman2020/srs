package com.gov.ac.feature.correspondence.security;

import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class CorrespondenceDepartmentAccessChecker {

  /** True when the viewer’s department matches the correspondence owning department. */
  public boolean isSameDepartmentAsOwner(AppUserEntity viewer, CorrespondenceEntity correspondence) {
    if (correspondence.getOwnerDepartment() == null) {
      return false;
    }
    return Objects.equals(
        viewer.getDepartment().getId(), correspondence.getOwnerDepartment().getId());
  }
}
