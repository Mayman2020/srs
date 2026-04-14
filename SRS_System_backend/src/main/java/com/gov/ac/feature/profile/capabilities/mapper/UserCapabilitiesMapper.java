package com.gov.ac.feature.profile.capabilities.mapper;

import com.gov.ac.feature.admin.entity.UiScreenEntity;
import com.gov.ac.feature.profile.capabilities.dto.CapabilityScreenDto;
import com.gov.ac.feature.profile.capabilities.dto.ShellNavItemDto;
import com.gov.ac.feature.profile.capabilities.dto.UserCapabilitiesDto;
import java.util.List;

public final class UserCapabilitiesMapper {

  private UserCapabilitiesMapper() {}

  public static ShellNavItemDto toNav(UiScreenEntity screen) {
    return new ShellNavItemDto(
        screen.getCode(),
        screen.getRoutePath(),
        screen.getNameAr(),
        screen.getNameEn(),
        screen.getSortOrder(),
        screen.getIconKey());
  }

  public static CapabilityScreenDto toCapabilityScreen(UiScreenEntity screen) {
    return new CapabilityScreenDto(screen.getCode(), screen.getRoutePath());
  }

  public static UserCapabilitiesDto toCapabilitiesDto(
      List<String> roleCodes, List<String> permissionCodes, List<CapabilityScreenDto> screens) {
    return new UserCapabilitiesDto(roleCodes, permissionCodes, screens);
  }
}
