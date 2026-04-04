package com.gov.ac.feature.users.service;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.domain.user.AppUser;
import com.gov.ac.domain.user.UserRole;
import com.gov.ac.domain.user.UserRoleId;
import com.gov.ac.feature.lookups.dto.LookupItemDto;
import com.gov.ac.feature.users.dto.UserListDto;
import com.gov.ac.feature.users.mapper.UserListMapper;
import com.gov.ac.persistence.AppUserRepository;
import com.gov.ac.persistence.RoleRepository;
import com.gov.ac.persistence.UserRoleRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAdminService {

  private final AppUserRepository appUserRepository;
  private final RoleRepository roleRepository;
  private final UserRoleRepository userRoleRepository;

  @Transactional(readOnly = true)
  public Page<UserListDto> listUsers(Pageable pageable) {
    return appUserRepository.findByDeletedAtIsNull(pageable).map(UserListMapper::toListDto);
  }

  @Transactional(readOnly = true)
  public List<LookupItemDto> listRoles() {
    return roleRepository.findByDeletedAtIsNullOrderBySortOrderAsc().stream()
        .map(
            r ->
                new LookupItemDto(
                    r.getId(), r.getCode(), r.getNameAr(), r.getNameEn(), r.getSortOrder()))
        .toList();
  }

  @Transactional
  public void assignRole(UUID actorId, UUID targetUserId, Long roleId) {
    AppUser user =
        appUserRepository
            .findByIdAndDeletedAtIsNull(targetUserId)
            .orElseThrow(() -> new NotFoundException("User not found"));
    if (!Boolean.TRUE.equals(user.getActive())) {
      throw new BadRequestException("Cannot assign roles to an inactive user");
    }
    roleRepository
        .findByIdAndDeletedAtIsNullAndActiveTrue(roleId)
        .orElseThrow(() -> new NotFoundException("Role not found"));

    UserRoleId id = new UserRoleId(targetUserId, roleId);
    Instant now = Instant.now();
    Optional<UserRole> existing = userRoleRepository.findById(id);
    if (existing.isPresent()) {
      UserRole ur = existing.get();
      if (ur.getValidTo() == null || ur.getValidTo().isAfter(now)) {
        return;
      }
      ur.setValidTo(null);
      ur.setValidFrom(now);
      ur.setUpdatedBy(actorId);
      userRoleRepository.save(ur);
      return;
    }

    UserRole ur = new UserRole();
    ur.setId(id);
    ur.setAppUser(appUserRepository.getReferenceById(targetUserId));
    ur.setRole(roleRepository.getReferenceById(roleId));
    ur.setValidFrom(now);
    ur.setValidTo(null);
    ur.setCreatedBy(actorId);
    ur.setUpdatedBy(actorId);
    userRoleRepository.save(ur);
  }
}
