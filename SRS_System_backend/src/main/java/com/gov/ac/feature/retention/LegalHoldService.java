package com.gov.ac.feature.retention;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.ForbiddenException;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.feature.retention.entity.LegalHoldEntity;
import com.gov.ac.feature.retention.repository.LegalHoldRepository;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LegalHoldService {

  private final LegalHoldRepository legalHoldRepository;
  private final CorrespondenceRepository correspondenceRepository;
  private final AppUserRepository appUserRepository;

  /** Throws when an active legal hold blocks mutating the given correspondence. */
  public void assertNotHeld(UUID correspondenceId) {
    if (legalHoldRepository.existsByCorrespondenceIsNullAndReleasedAtIsNullAndDeletedAtIsNull()) {
      throw new BadRequestException("A blanket legal hold is in effect");
    }
    if (correspondenceId != null
        && legalHoldRepository.existsByCorrespondence_IdAndReleasedAtIsNullAndDeletedAtIsNull(
            correspondenceId)) {
      throw new BadRequestException("This correspondence is under legal hold");
    }
  }

  @Transactional(readOnly = true)
  public List<LegalHoldEntity> listActive() {
    return legalHoldRepository.findByReleasedAtIsNullAndDeletedAtIsNullOrderByPlacedAtDesc();
  }

  @Transactional(readOnly = true)
  public List<LegalHoldEntity> listActiveForCorrespondence(UUID correspondenceId) {
    return legalHoldRepository
        .findByCorrespondence_IdAndReleasedAtIsNullAndDeletedAtIsNullOrderByPlacedAtDesc(
            correspondenceId);
  }

  @Transactional
  public LegalHoldEntity place(UUID actorUserId, UUID correspondenceId, String reason) {
    if (reason == null || reason.isBlank()) {
      throw new BadRequestException("reason is required");
    }
    requireActiveUser(actorUserId);
    LegalHoldEntity row = new LegalHoldEntity();
    if (correspondenceId != null) {
      CorrespondenceEntity c =
          correspondenceRepository
              .findById(correspondenceId)
              .orElseThrow(() -> new NotFoundException("Correspondence not found"));
      row.setCorrespondence(c);
    } else {
      row.setCorrespondence(null);
    }
    row.setReason(reason);
    row.setPlacedBy(actorUserId);
    row.setPlacedAt(Instant.now());
    row.setCreatedBy(actorUserId);
    row.setUpdatedBy(actorUserId);
    return legalHoldRepository.save(row);
  }

  @Transactional
  public void release(UUID actorUserId, UUID holdId, String releaseReason) {
    if (releaseReason == null || releaseReason.isBlank()) {
      throw new BadRequestException("releaseReason is required");
    }
    requireActiveUser(actorUserId);
    LegalHoldEntity row =
        legalHoldRepository.findById(holdId).orElseThrow(() -> new NotFoundException("Hold not found"));
    if (row.getReleasedAt() != null) {
      return;
    }
    row.setReleasedAt(Instant.now());
    row.setReleasedBy(actorUserId);
    row.setReleaseReason(releaseReason);
    row.setUpdatedBy(actorUserId);
    legalHoldRepository.save(row);
  }

  public boolean hasActiveBlanketHold() {
    return legalHoldRepository.existsByCorrespondenceIsNullAndReleasedAtIsNullAndDeletedAtIsNull();
  }

  private void requireActiveUser(UUID userId) {
    AppUserEntity u =
        appUserRepository
            .findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new ForbiddenException("You do not have access"));
    if (!Boolean.TRUE.equals(u.getActive())) {
      throw new ForbiddenException("You do not have access");
    }
  }
}
