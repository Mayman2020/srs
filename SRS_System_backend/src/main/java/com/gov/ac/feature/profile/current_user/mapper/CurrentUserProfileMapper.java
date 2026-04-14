package com.gov.ac.feature.profile.current_user.mapper;

import com.gov.ac.feature.departments.entity.DepartmentEntity;
import com.gov.ac.feature.profile.current_user.dto.CurrentUserProfileDto;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.roles.repository.RoleRepository;
import com.gov.ac.feature.users.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserProfileMapper {

  private final UserRoleRepository userRoleRepository;
  private final RoleRepository roleRepository;

  public CurrentUserProfileDto toDto(AppUserEntity user) {
    DepartmentEntity department = user.getDepartment();
    return new CurrentUserProfileDto(
        user.getId(),
        user.getUsername(),
        user.getFullNameAr(),
        user.getFullNameEn(),
        user.getEmail(),
        user.getPhone(),
        user.getNationalId(),
        department != null ? department.getId() : null,
        department != null ? department.getCode() : null,
        department != null ? department.getNameAr() : null,
        department != null ? department.getNameEn() : null,
        user.getActive(),
        user.getMfaEnabled(),
        user.getLastLoginAt(),
        user.getPasswordChangedAt(),
        userRoleRepository.findActiveRoleIdsByUserId(user.getId()),
        roleRepository.findActiveRoleCodesByUserId(user.getId()),
        profileImageUrl(user),
        user.getUiTheme(),
        user.getUiLocale());
  }

  private String profileImageUrl(AppUserEntity user) {
    if (normalizeNullable(user.getProfileImagePath()) == null) {
      return null;
    }
    long version =
        user.getUpdatedAt() != null ? user.getUpdatedAt().toEpochMilli() : System.currentTimeMillis();
    return "/api/v1/profile/me/avatar?v=" + version;
  }

  private String normalizeNullable(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    return normalized.isBlank() ? null : normalized;
  }
}
