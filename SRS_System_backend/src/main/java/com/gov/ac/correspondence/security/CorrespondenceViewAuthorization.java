package com.gov.ac.correspondence.security;

import com.gov.ac.domain.correspondence.Correspondence;
import com.gov.ac.domain.user.AppUser;
import com.gov.ac.common.api.ForbiddenException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * View authorization for a single correspondence. Access when any rule matches:
 *
 * <ol>
 *   <li>User’s department equals {@code owner_department_id}
 *   <li>User participates in Camunda workflow (business key = {@code reference_number})
 *   <li>User has a correspondence view-any role ({@link com.gov.ac.security.rbac.RbacRoleCodes#CORRESPONDENCE_VIEW_ANY})
 * </ol>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CorrespondenceViewAuthorization {

  private final CorrespondencePrivilegedRoleChecker privilegedRoleChecker;
  private final CorrespondenceDepartmentAccessChecker departmentAccessChecker;
  private final CorrespondenceWorkflowParticipationChecker workflowParticipationChecker;

  public void assertCanView(AppUser viewer, Correspondence correspondence) {
    UUID userId = viewer.getId();
    UUID correspondenceId = correspondence.getId();
    String referenceNumber = correspondence.getReferenceNumber();

    if (departmentAccessChecker.isSameDepartmentAsOwner(viewer, correspondence)) {
      log.debug(
          "Correspondence view allowed (same department as owner): userId={} correspondenceId={}",
          userId,
          correspondenceId);
      return;
    }

    if (workflowParticipationChecker.isParticipating(userId, referenceNumber)) {
      log.debug(
          "Correspondence view allowed (workflow): userId={} correspondenceId={}",
          userId,
          correspondenceId);
      return;
    }

    if (privilegedRoleChecker.hasPrivilegedViewRole(userId)) {
      log.debug(
          "Correspondence view allowed (privileged role): userId={} correspondenceId={}",
          userId,
          correspondenceId);
      return;
    }

    log.warn(
        "Correspondence view denied: userId={} correspondenceId={} referenceNumber={} (not same department as owner, not workflow participant, no privileged view role)",
        userId,
        correspondenceId,
        referenceNumber);
    throw new ForbiddenException("You do not have access to this correspondence");
  }
}
