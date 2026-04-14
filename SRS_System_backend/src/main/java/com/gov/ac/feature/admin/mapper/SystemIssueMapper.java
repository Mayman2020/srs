package com.gov.ac.feature.admin.mapper;

import com.gov.ac.feature.admin.dto.SystemIssueDto;
import com.gov.ac.feature.admin.entity.SystemIssueEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;

public final class SystemIssueMapper {

  private SystemIssueMapper() {}

  public static SystemIssueDto toDto(SystemIssueEntity issue) {
    AppUserEntity user = issue.getUser();
    return new SystemIssueDto(
        issue.getId(),
        issue.getSource(),
        issue.getSeverity(),
        issue.getMessage(),
        issue.getDetail(),
        issue.getPageUrl(),
        user != null ? user.getId() : null,
        issue.getHttpStatus(),
        issue.getCreatedAt(),
        issue.getResolvedAt(),
        issue.getResolvedBy(),
        issue.getResolutionNote());
  }
}
