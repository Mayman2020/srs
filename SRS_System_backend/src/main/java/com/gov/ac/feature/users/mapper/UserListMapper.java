package com.gov.ac.feature.users.mapper;

import com.gov.ac.feature.departments.entity.DepartmentEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.dto.UserListDto;

public final class UserListMapper {

  private UserListMapper() {}

  public static UserListDto toListDto(AppUserEntity u) {
    DepartmentEntity d = u.getDepartment();
    String deptCode = d != null ? d.getCode() : null;
    return new UserListDto(
        u.getId(),
        u.getUsername(),
        u.getFullNameAr(),
        u.getFullNameEn(),
        u.getEmail(),
        deptCode,
        u.getActive());
  }
}
