package com.gov.ac.feature.delegation.service;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.ForbiddenException;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.feature.delegation.entity.AuthorityDelegationEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.delegation.dto.AuthorityDelegationDto;
import com.gov.ac.feature.delegation.dto.CreateAuthorityDelegationRequestDto;
import com.gov.ac.feature.delegation.mapper.AuthorityDelegationMapper;
import com.gov.ac.feature.users.repository.AppUserRepository;
import com.gov.ac.feature.delegation.repository.AuthorityDelegationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthorityDelegationService {

  private final AuthorityDelegationRepository authorityDelegationRepository;
  private final AppUserRepository appUserRepository;

  @Transactional(readOnly = true)
  public List<AuthorityDelegationDto> listForCurrentUser(UUID userId) {
    return authorityDelegationRepository.findVisibleForUser(userId).stream()
        .map(AuthorityDelegationMapper::toDto)
        .toList();
  }

  @Transactional
  public AuthorityDelegationDto create(UUID currentUserId, CreateAuthorityDelegationRequestDto req) {
    if (req.delegateUserId().equals(currentUserId)) {
      throw new BadRequestException("Cannot delegate to yourself");
    }
    if (req.validTo().isBefore(req.validFrom())) {
      throw new BadRequestException("validTo must be on or after validFrom");
    }
    AppUserEntity delegator =
        appUserRepository
            .findByIdAndDeletedAtIsNull(currentUserId)
            .orElseThrow(() -> new NotFoundException("User not found"));
    if (!Boolean.TRUE.equals(delegator.getActive())) {
      throw new BadRequestException("Inactive user cannot create delegations");
    }
    AppUserEntity delegate =
        appUserRepository
            .findByIdAndDeletedAtIsNull(req.delegateUserId())
            .orElseThrow(() -> new BadRequestException("Unknown delegate user"));
    if (!Boolean.TRUE.equals(delegate.getActive())) {
      throw new BadRequestException("Inactive delegate user");
    }

    AuthorityDelegationEntity d = new AuthorityDelegationEntity();
    d.setDelegatorUser(delegator);
    d.setDelegateUser(delegate);
    d.setValidFrom(req.validFrom());
    d.setValidTo(req.validTo());
    d.setAllowedCorrespondenceTypeCodes(req.allowedCorrespondenceTypeCodes());
    d.setAllowedConfidentialityCodes(req.allowedConfidentialityCodes());
    d.setCanSignOnBehalf(Boolean.TRUE.equals(req.canSignOnBehalf()));
    d.setNotes(req.notes());
    d.setCreatedBy(currentUserId);
    d.setUpdatedBy(currentUserId);
    return AuthorityDelegationMapper.toDto(authorityDelegationRepository.save(d));
  }

  @Transactional
  public void revoke(UUID currentUserId, UUID delegationId, boolean mayRevokeAsManager) {
    AuthorityDelegationEntity d =
        authorityDelegationRepository
            .findByIdAndDeletedAtIsNull(delegationId)
            .orElseThrow(() -> new NotFoundException("Delegation not found"));
    boolean isDelegator = d.getDelegatorUser().getId().equals(currentUserId);
    if (!isDelegator && !mayRevokeAsManager) {
      throw new ForbiddenException("Only the delegator (or an administrator) can revoke this delegation");
    }
    d.setDeletedAt(Instant.now());
    d.setDeletedBy(currentUserId);
    d.setUpdatedBy(currentUserId);
  }
}
