package com.gov.ac.feature.admin.mapper;

import com.gov.ac.feature.admin.dto.PermissionDto;
import com.gov.ac.feature.admin.dto.UiScreenDto;
import com.gov.ac.feature.admin.entity.UiScreenEntity;
import com.gov.ac.feature.roles.entity.PermissionEntity;

public final class AdminConsoleMapper {

  private AdminConsoleMapper() {}

  public static PermissionDto toPermissionDto(PermissionEntity permission) {
    return new PermissionDto(
        permission.getId(),
        permission.getCode(),
        permission.getNameAr(),
        permission.getNameEn(),
        permission.getDescription(),
        permission.getSortOrder(),
        permission.getActive(),
        permission.getUiScreenId());
  }

  public static UiScreenDto toUiScreenDto(UiScreenEntity screen) {
    return new UiScreenDto(
        screen.getId(),
        screen.getCode(),
        screen.getRoutePath(),
        screen.getNameAr(),
        screen.getNameEn(),
        screen.getDescription(),
        screen.getSortOrder(),
        screen.getActive(),
        screen.getRequiredPermissionId(),
        screen.getIconKey(),
        screen.getShowInShellNav());
  }
}
