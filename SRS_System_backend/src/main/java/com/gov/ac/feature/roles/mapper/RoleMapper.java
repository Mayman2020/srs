package com.gov.ac.feature.roles.mapper;

import com.gov.ac.feature.lookups.dto.LookupItemDto;
import com.gov.ac.feature.roles.dto.RoleOptionDto;
import com.gov.ac.feature.roles.entity.RoleEntity;

public final class RoleMapper {

  private RoleMapper() {}

  public static RoleOptionDto toOptionDto(RoleEntity role) {
    return new RoleOptionDto(
        role.getId(), role.getCode(), role.getNameAr(), role.getNameEn(), role.getSortOrder(), null);
  }

  public static LookupItemDto toLookupItem(RoleEntity role) {
    return new LookupItemDto(
        role.getId(), role.getCode(), role.getNameAr(), role.getNameEn(), role.getSortOrder(), null);
  }
}
