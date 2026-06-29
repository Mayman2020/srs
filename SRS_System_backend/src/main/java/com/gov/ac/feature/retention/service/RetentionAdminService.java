package com.gov.ac.feature.retention.service;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.feature.retention.dto.ArchiveTransitionLogDto;
import com.gov.ac.feature.retention.dto.LegalHoldDto;
import com.gov.ac.feature.retention.dto.LegalHoldPlaceRequestDto;
import com.gov.ac.feature.retention.dto.LegalHoldReleaseRequestDto;
import com.gov.ac.feature.retention.dto.RetentionPolicyAdminDto;
import com.gov.ac.feature.retention.dto.RetentionPolicyToggleRequestDto;
import com.gov.ac.feature.retention.entity.ArchiveTransitionLogEntity;
import com.gov.ac.feature.retention.entity.LegalHoldEntity;
import com.gov.ac.feature.retention.entity.RetentionPolicyEntity;
import com.gov.ac.feature.retention.repository.ArchiveTransitionLogRepository;
import com.gov.ac.feature.retention.repository.RetentionPolicyRepository;
import com.gov.ac.feature.retention.LegalHoldService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RetentionAdminService {

  private final RetentionPolicyRepository policyRepository;
  private final LegalHoldService legalHoldService;
  private final ArchiveTransitionLogRepository archiveTransitionLogRepository;

  @Transactional(readOnly = true)
  public List<RetentionPolicyAdminDto> listPolicies() {
    return policyRepository.findByDeletedAtIsNullOrderByCodeAsc().stream()
        .map(RetentionAdminService::toPolicyDto)
        .toList();
  }

  public RetentionPolicyAdminDto togglePolicy(UUID id, RetentionPolicyToggleRequestDto body) {
    if (body == null || body.enabled() == null) {
      throw new BadRequestException("enabled is required");
    }
    RetentionPolicyEntity p =
        policyRepository.findById(id).orElseThrow(() -> new NotFoundException("Policy not found"));
    p.setEnabled(body.enabled());
    return toPolicyDto(policyRepository.save(p));
  }

  @Transactional(readOnly = true)
  public List<LegalHoldDto> listActiveHolds() {
    return legalHoldService.listActive().stream().map(RetentionAdminService::toHoldDto).toList();
  }

  @Transactional(readOnly = true)
  public List<LegalHoldDto> listActiveHoldsForCorrespondence(UUID correspondenceId) {
    return legalHoldService.listActiveForCorrespondence(correspondenceId).stream()
        .map(RetentionAdminService::toHoldDto)
        .toList();
  }

  @Transactional
  public LegalHoldDto placeHold(UUID actor, LegalHoldPlaceRequestDto body) {
    LegalHoldEntity saved = legalHoldService.place(actor, body.correspondenceId(), body.reason());
    return toHoldDto(saved);
  }

  @Transactional
  public void releaseHold(UUID actor, UUID holdId, LegalHoldReleaseRequestDto body) {
    legalHoldService.release(actor, holdId, body.releaseReason());
  }

  @Transactional(readOnly = true)
  public Page<ArchiveTransitionLogDto> pageLog(Pageable pageable) {
    return archiveTransitionLogRepository.findAllByOrderByExecutedAtDesc(pageable).map(RetentionAdminService::toLogDto);
  }

  private static RetentionPolicyAdminDto toPolicyDto(RetentionPolicyEntity p) {
    return new RetentionPolicyAdminDto(
        p.getId(),
        p.getCode(),
        p.getNameEn(),
        p.getNameAr(),
        p.getAppliesTo(),
        p.getRetainForDays(),
        p.getActionAfter(),
        p.getEnabled());
  }

  private static LegalHoldDto toHoldDto(LegalHoldEntity h) {
    return new LegalHoldDto(
        h.getId(),
        h.getCorrespondence() != null ? h.getCorrespondence().getId() : null,
        h.getReason(),
        h.getPlacedBy(),
        h.getPlacedAt(),
        h.getReleasedAt(),
        h.getReleasedBy(),
        h.getReleaseReason());
  }

  private static ArchiveTransitionLogDto toLogDto(ArchiveTransitionLogEntity e) {
    return new ArchiveTransitionLogDto(
        e.getId(),
        e.getAppliedTo(),
        e.getResourceId(),
        e.getPolicy() != null ? e.getPolicy().getId() : null,
        e.getAction(),
        e.getExecutedAt(),
        e.getDetailJson());
  }
}
