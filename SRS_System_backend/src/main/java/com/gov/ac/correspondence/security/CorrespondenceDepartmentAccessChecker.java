package com.gov.ac.correspondence.security;

import com.gov.ac.domain.correspondence.Correspondence;
import com.gov.ac.domain.user.AppUser;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class CorrespondenceDepartmentAccessChecker {

  /** True when the viewer’s department matches the correspondence owning department. */
  public boolean isSameDepartmentAsOwner(AppUser viewer, Correspondence correspondence) {
    if (correspondence.getOwnerDepartment() == null) {
      return false;
    }
    return Objects.equals(
        viewer.getDepartment().getId(), correspondence.getOwnerDepartment().getId());
  }
}
