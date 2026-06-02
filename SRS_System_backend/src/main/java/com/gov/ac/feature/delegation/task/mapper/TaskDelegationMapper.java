package com.gov.ac.feature.delegation.task.mapper;

import com.gov.ac.feature.correspondence.dto.UserSummaryDto;
import com.gov.ac.feature.delegation.task.dto.TaskDelegationDto;
import com.gov.ac.feature.delegation.task.entity.TaskDelegationEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;
import java.time.LocalDate;

public final class TaskDelegationMapper {

  private TaskDelegationMapper() {}

  public static TaskDelegationDto toDto(TaskDelegationEntity entity, LocalDate today) {
    return new TaskDelegationDto(
        entity.getId(),
        toUserSummary(entity.getDelegatorUser()),
        toUserSummary(entity.getDelegateUser()),
        entity.getScopeType(),
        entity.getCorrespondenceId(),
        entity.getCamundaTaskId(),
        entity.getProcessInstanceId(),
        entity.getAllowedCorrespondenceTypeCodes(),
        entity.getAllowedConfidentialityCodes(),
        entity.getValidFrom(),
        entity.getValidTo(),
        entity.getNotes(),
        entity.getRevokedAt(),
        entity.getRevokedBy(),
        entity.getAuthorityDelegation() != null ? entity.getAuthorityDelegation().getId() : null,
        entity.isActiveOn(today));
  }

  private static UserSummaryDto toUserSummary(AppUserEntity user) {
    if (user == null) {
      return null;
    }
    return UserSummaryDto.builder()
        .id(user.getId())
        .username(user.getUsername())
        .fullNameAr(user.getFullNameAr())
        .fullNameEn(user.getFullNameEn())
        .build();
  }
}
