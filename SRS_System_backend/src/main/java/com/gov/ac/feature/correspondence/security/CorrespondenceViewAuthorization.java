package com.gov.ac.feature.correspondence.security;

import com.gov.ac.common.api.ForbiddenException;
import com.gov.ac.common.i18n.Messages;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.lookups.entity.ConfidentialityEntity;
import com.gov.ac.feature.lookups.repository.ConfidentialityRepository;
import com.gov.ac.feature.users.entity.AppUserEntity;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * View authorization for a single correspondence. Access requires BOTH:
 *
 * <ol>
 *   <li>At least one of:
 *       <ul>
 *         <li>User's department equals {@code owner_department_id}
 *         <li>User participates in the Camunda workflow (business key = {@code reference_number})
 *         <li>User has a correspondence view-any role ({@link
 *             com.gov.ac.security.rbac.RbacRoleCodes#CORRESPONDENCE_VIEW_ANY})
 *       </ul>
 *   <li>Security clearance >= correspondence confidentiality when {@code requires_clearance} is
 *       TRUE on the level (TOP_SECRET, ...).
 * </ol>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CorrespondenceViewAuthorization {

  private final CorrespondencePrivilegedRoleChecker privilegedRoleChecker;
  private final CorrespondenceDepartmentAccessChecker departmentAccessChecker;
  private final CorrespondenceWorkflowParticipationChecker workflowParticipationChecker;
  private final ConfidentialityRepository confidentialityRepository;
  private final Messages messages;

  public void assertCanView(AppUserEntity viewer, CorrespondenceEntity correspondence) {
    UUID userId = viewer.getId();
    UUID correspondenceId = correspondence.getId();
    String referenceNumber = correspondence.getReferenceNumber();

    boolean baseAccess =
        departmentAccessChecker.isSameDepartmentAsOwner(viewer, correspondence)
            || workflowParticipationChecker.isParticipating(userId, referenceNumber)
            || privilegedRoleChecker.hasPrivilegedViewRole(userId);

    if (!baseAccess) {
      log.warn(
          "Correspondence view denied: userId={} correspondenceId={} referenceNumber={} (not same department, not workflow participant, no privileged view role)",
          userId,
          correspondenceId,
          referenceNumber);
      throw new ForbiddenException(messages.get("correspondence.access.denied"));
    }

    assertClearance(viewer, correspondence);

    log.debug(
        "Correspondence view allowed: userId={} correspondenceId={}", userId, correspondenceId);
  }

  /**
   * Confidentiality enforcement: when the correspondence's confidentiality requires clearance,
   * the user's clearance must be at least as restrictive (numerically lower {@code sort_order}
   * means more restrictive) as the correspondence level. Users without an assigned clearance are
   * blocked from restricted material.
   */
  private void assertClearance(AppUserEntity viewer, CorrespondenceEntity correspondence) {
    ConfidentialityEntity level = correspondence.getConfidentiality();
    if (level == null || !Boolean.TRUE.equals(level.getRequiresClearance())) {
      return;
    }
    Long userClearanceId = viewer.getSecurityClearanceId();
    if (userClearanceId == null) {
      throw new ForbiddenException(messages.get("correspondence.clearance.required"));
    }
    ConfidentialityEntity userClearance =
        confidentialityRepository
            .findByIdAndDeletedAtIsNull(userClearanceId)
            .orElse(null);
    if (userClearance == null) {
      throw new ForbiddenException(messages.get("correspondence.clearance.required"));
    }
    // Lower sort_order = more restrictive level (matches V1 seed: TOP_SECRET=10, NORMAL=50).
    int userOrder = userClearance.getSortOrder() == null ? Integer.MAX_VALUE : userClearance.getSortOrder();
    int requiredOrder = level.getSortOrder() == null ? Integer.MAX_VALUE : level.getSortOrder();
    if (userOrder > requiredOrder) {
      log.warn(
          "Clearance denied: userId={} userClearance={} (sort={}) correspondence={} required={} (sort={})",
          viewer.getId(),
          userClearance.getCode(),
          userOrder,
          correspondence.getId(),
          level.getCode(),
          requiredOrder);
      throw new ForbiddenException(messages.get("correspondence.clearance.required"));
    }
  }
}
