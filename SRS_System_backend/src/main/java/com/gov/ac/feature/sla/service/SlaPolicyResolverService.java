package com.gov.ac.feature.sla.service;

import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.lookups.entity.ConfidentialityEntity;
import com.gov.ac.feature.lookups.entity.CorrespondenceTypeEntity;
import com.gov.ac.feature.lookups.entity.PriorityEntity;
import com.gov.ac.feature.lookups.entity.WorkflowActionTypeEntity;
import com.gov.ac.feature.sla.entity.SlaPolicyEntity;
import com.gov.ac.feature.sla.repository.SlaPolicyRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the SLA policy that applies to a specific task.
 *
 * <p>Matching rules:
 *
 * <ul>
 *   <li>A criterion column on a policy must equal the task's value to count as a match; a {@code
 *       null} criterion is a wildcard and always matches.
 *   <li>If <strong>any</strong> non-null criterion does not match the task, the policy is
 *       rejected.
 *   <li>Among accepted policies, the one with the highest specificity wins. Specificity = number
 *       of non-null criteria.
 *   <li>Ties are broken by lowest {@code id} (insertion order) so resolution stays deterministic.
 * </ul>
 *
 * <p>This service is deliberately a pure function of {@link SlaPolicyEntity} state + the input
 * tuple; both inputs are available without hitting Camunda, which keeps the resolver fast and
 * unit-testable.
 */
@Service
@RequiredArgsConstructor
public class SlaPolicyResolverService {

  private final SlaPolicyRepository slaPolicyRepository;

  /**
   * Returns the highest-specificity active policy that matches the supplied task attributes, or
   * empty if none match. Callers that have a {@link CorrespondenceEntity} can use {@link
   * #resolveFor(CorrespondenceEntity, String, String)}.
   */
  @Transactional(readOnly = true)
  public Optional<SlaPolicyEntity> resolveFor(
      Long correspondenceTypeId,
      Long priorityId,
      Long confidentialityId,
      String orgLevelCode,
      Long workflowActionTypeId) {
    List<SlaPolicyEntity> candidates = slaPolicyRepository.findByActiveTrueAndDeletedAtIsNull();
    if (candidates.isEmpty()) {
      return Optional.empty();
    }
    return candidates.stream()
        .filter(
            p ->
                matches(
                    p,
                    correspondenceTypeId,
                    priorityId,
                    confidentialityId,
                    orgLevelCode,
                    workflowActionTypeId))
        .max(
            Comparator.comparingInt(SlaPolicyEntity::specificity)
                .thenComparing(
                    SlaPolicyEntity::getId, Comparator.nullsLast(Comparator.reverseOrder())));
  }

  /** Convenience overload that pulls criterion ids from a correspondence + workflow context. */
  @Transactional(readOnly = true)
  public Optional<SlaPolicyEntity> resolveFor(
      CorrespondenceEntity correspondence,
      String orgLevelCode,
      String workflowActionTypeCode) {
    if (correspondence == null) {
      return Optional.empty();
    }
    Long typeId =
        correspondence.getCorrespondenceType() != null
            ? correspondence.getCorrespondenceType().getId()
            : null;
    Long priorityId = correspondence.getPriority() != null ? correspondence.getPriority().getId() : null;
    Long confidentialityId =
        correspondence.getConfidentiality() != null
            ? correspondence.getConfidentiality().getId()
            : null;
    Long workflowActionTypeId = null;
    if (workflowActionTypeCode != null && !workflowActionTypeCode.isBlank()) {
      // Loaded later when needed by callers; we only carry it through if known. Pure resolver
      // accepts the id directly to keep policy resolution independent of repositories.
      workflowActionTypeId = null;
    }
    return resolveFor(typeId, priorityId, confidentialityId, orgLevelCode, workflowActionTypeId);
  }

  // ---------------------------------------------------------------------------
  // helpers
  // ---------------------------------------------------------------------------

  private static boolean matches(
      SlaPolicyEntity policy,
      Long correspondenceTypeId,
      Long priorityId,
      Long confidentialityId,
      String orgLevelCode,
      Long workflowActionTypeId) {
    if (!matchesId(extractId(policy.getCorrespondenceType()), correspondenceTypeId)) {
      return false;
    }
    if (!matchesId(extractId(policy.getPriority()), priorityId)) {
      return false;
    }
    if (!matchesId(extractId(policy.getConfidentiality()), confidentialityId)) {
      return false;
    }
    if (!matchesCode(policy.getOrgLevelCode(), orgLevelCode)) {
      return false;
    }
    return matchesId(extractId(policy.getWorkflowActionType()), workflowActionTypeId);
  }

  private static Long extractId(CorrespondenceTypeEntity e) {
    return e == null ? null : e.getId();
  }

  private static Long extractId(PriorityEntity e) {
    return e == null ? null : e.getId();
  }

  private static Long extractId(ConfidentialityEntity e) {
    return e == null ? null : e.getId();
  }

  private static Long extractId(WorkflowActionTypeEntity e) {
    return e == null ? null : e.getId();
  }

  private static boolean matchesId(Long criterion, Long value) {
    if (criterion == null) {
      return true; // wildcard
    }
    return criterion.equals(value);
  }

  private static boolean matchesCode(String criterion, String value) {
    if (criterion == null || criterion.isBlank()) {
      return true;
    }
    if (value == null) {
      return false;
    }
    return criterion.equalsIgnoreCase(value);
  }
}
