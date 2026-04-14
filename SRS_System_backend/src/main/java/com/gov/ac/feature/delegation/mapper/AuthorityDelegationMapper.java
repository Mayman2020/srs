package com.gov.ac.feature.delegation.mapper;

import com.gov.ac.feature.correspondence.dto.UserSummaryDto;
import com.gov.ac.feature.delegation.dto.AuthorityDelegationDto;
import com.gov.ac.feature.delegation.entity.AuthorityDelegationEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;

public final class AuthorityDelegationMapper {

  private AuthorityDelegationMapper() {}

  public static AuthorityDelegationDto toDto(AuthorityDelegationEntity delegation) {
    return new AuthorityDelegationDto(
        delegation.getId(),
        toUserSummary(delegation.getDelegatorUser()),
        toUserSummary(delegation.getDelegateUser()),
        delegation.getValidFrom(),
        delegation.getValidTo(),
        delegation.getAllowedCorrespondenceTypeCodes(),
        delegation.getAllowedConfidentialityCodes(),
        Boolean.TRUE.equals(delegation.getCanSignOnBehalf()),
        delegation.getNotes());
  }

  private static UserSummaryDto toUserSummary(AppUserEntity user) {
    return UserSummaryDto.builder()
        .id(user.getId())
        .username(user.getUsername())
        .fullNameAr(user.getFullNameAr())
        .fullNameEn(user.getFullNameEn())
        .build();
  }
}
