package com.gov.ac.feature.users.mapper;

import com.gov.ac.common.audit.UserAuditRefDto;
import com.gov.ac.feature.departments.entity.DepartmentEntity;
import com.gov.ac.feature.users.dto.UserDetailDto;
import com.gov.ac.feature.users.entity.AppUserEntity;
import java.util.List;

public final class UserAdminMapper {

  private UserAdminMapper() {}

  public static UserDetailDto toDetailDto(
      AppUserEntity user,
      List<Long> roleIds,
      UserAuditRefDto createdByUser,
      UserAuditRefDto updatedByUser) {
    DepartmentEntity department = user.getDepartment();
    return new UserDetailDto(
        user.getId(),
        user.getUsername(),
        user.getFullNameAr(),
        user.getFullNameEn(),
        user.getEmail(),
        department != null ? department.getCode() : null,
        department != null ? department.getId() : null,
        user.getActive(),
        user.getMustChangePassword(),
        roleIds,
        user.getSecurityClearanceId(),
        user.getCreatedAt(),
        user.getUpdatedAt(),
        createdByUser,
        updatedByUser);
  }
}
