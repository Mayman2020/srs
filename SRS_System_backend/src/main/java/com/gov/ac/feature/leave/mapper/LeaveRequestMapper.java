package com.gov.ac.feature.leave.mapper;

import com.gov.ac.feature.leave.dto.LeaveRequestDto;
import com.gov.ac.feature.leave.entity.LeaveRequestEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;

public final class LeaveRequestMapper {

  private LeaveRequestMapper() {}

  public static LeaveRequestDto toDto(LeaveRequestEntity leaveRequest) {
    AppUserEntity user = leaveRequest.getUser();
    AppUserEntity decider = leaveRequest.getDecidedBy();
    return new LeaveRequestDto(
        leaveRequest.getId(),
        user != null ? user.getId() : null,
        user != null ? user.getUsername() : null,
        user != null ? user.getFullNameAr() : null,
        user != null ? user.getFullNameEn() : null,
        leaveRequest.getStartDate(),
        leaveRequest.getEndDate(),
        leaveRequest.getReason(),
        leaveRequest.getStatus() != null ? leaveRequest.getStatus().getCode() : leaveRequest.getStatusCode(),
        leaveRequest.getStatus() != null ? leaveRequest.getStatus().getUiVariant() : null,
        decider != null ? decider.getId() : null,
        leaveRequest.getDecidedAt(),
        leaveRequest.getDecisionNote(),
        leaveRequest.getCreatedAt());
  }
}
