package com.gov.ac.feature.users.mapper;

import com.gov.ac.domain.org.Department;
import com.gov.ac.domain.user.AppUser;
import com.gov.ac.feature.users.dto.UserListDto;

public final class UserListMapper {

  private UserListMapper() {}

  public static UserListDto toListDto(AppUser u) {
    Department d = u.getDepartment();
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
