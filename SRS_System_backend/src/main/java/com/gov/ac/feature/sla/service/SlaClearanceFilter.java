package com.gov.ac.feature.sla.service;

import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.lookups.entity.ConfidentialityEntity;
import com.gov.ac.feature.lookups.repository.ConfidentialityRepository;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Confidentiality filter for SLA escalation actions. Mirrors the {@code
 * CorrespondenceViewAuthorization} clearance rule: lower {@code sort_order} = more restrictive,
 * and the candidate user must have {@code user.sortOrder <= correspondence.sortOrder} to receive
 * the escalation signal (notification or reassignment).
 *
 * <p>This is the hard rule that makes "escalation does not bypass confidentiality clearance"
 * enforceable from a single call site. Every {@link SlaEscalationService} action funnels its
 * candidate set through {@link #filter(CorrespondenceEntity, Collection)} before doing anything
 * visible to the user.
 */
@Component
@RequiredArgsConstructor
public class SlaClearanceFilter {

  private final AppUserRepository appUserRepository;
  private final ConfidentialityRepository confidentialityRepository;

  @Transactional(readOnly = true)
  public List<UUID> filter(CorrespondenceEntity correspondence, Collection<UUID> candidateIds) {
    if (candidateIds == null || candidateIds.isEmpty()) {
      return List.of();
    }
    ConfidentialityEntity level = correspondence == null ? null : correspondence.getConfidentiality();
    boolean requiresClearance = level != null && Boolean.TRUE.equals(level.getRequiresClearance());
    int requiredOrder =
        requiresClearance && level.getSortOrder() != null ? level.getSortOrder() : Integer.MAX_VALUE;
    if (!requiresClearance) {
      return new ArrayList<>(candidateIds);
    }
    List<UUID> allowed = new ArrayList<>(candidateIds.size());
    for (UUID candidateId : candidateIds) {
      if (candidateId == null) {
        continue;
      }
      AppUserEntity user = appUserRepository.findByIdAndDeletedAtIsNull(candidateId).orElse(null);
      if (user == null) {
        continue;
      }
      Long clearanceId = user.getSecurityClearanceId();
      if (clearanceId == null) {
        continue; // no clearance => cannot receive restricted material
      }
      ConfidentialityEntity userClearance =
          confidentialityRepository.findByIdAndDeletedAtIsNull(clearanceId).orElse(null);
      if (userClearance == null) {
        continue;
      }
      int userOrder =
          userClearance.getSortOrder() == null ? Integer.MAX_VALUE : userClearance.getSortOrder();
      if (userOrder <= requiredOrder) {
        allowed.add(candidateId);
      }
    }
    return allowed;
  }

  /** Whether the user identified by {@code candidateId} satisfies the correspondence clearance. */
  @Transactional(readOnly = true)
  public boolean isCleared(CorrespondenceEntity correspondence, UUID candidateId) {
    if (candidateId == null) {
      return false;
    }
    return !filter(correspondence, List.of(candidateId)).isEmpty();
  }
}
