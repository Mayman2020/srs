package com.gov.ac.feature.users.service;

import com.gov.ac.common.audit.UserAuditRefDto;
import com.gov.ac.common.audit.UserAuditResolutionService;
import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.feature.departments.entity.DepartmentEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.roles.entity.RoleEntity;
import com.gov.ac.feature.users.entity.UserRoleEntity;
import com.gov.ac.feature.users.entity.UserRoleId;
import com.gov.ac.feature.lookups.dto.LookupItemDto;
import com.gov.ac.feature.roles.mapper.RoleMapper;
import com.gov.ac.feature.users.dto.CreateAppUserRequestDto;
import com.gov.ac.feature.users.dto.UpdateAppUserRequestDto;
import com.gov.ac.feature.users.dto.UserDetailDto;
import com.gov.ac.feature.users.dto.EffectivePermissionDto;
import com.gov.ac.security.permission.EffectiveUserPermissionService;
import com.gov.ac.feature.roles.repository.PermissionRepository;
import com.gov.ac.feature.users.dto.UserListDto;
import com.gov.ac.feature.lookups.repository.ConfidentialityRepository;
import com.gov.ac.feature.users.mapper.UserAdminMapper;
import com.gov.ac.feature.users.mapper.UserListMapper;
import com.gov.ac.feature.users.repository.AppUserRepository;
import com.gov.ac.feature.departments.repository.DepartmentRepository;
import com.gov.ac.feature.roles.repository.RoleRepository;
import com.gov.ac.feature.users.repository.UserRoleRepository;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAdminService {

  private final AppUserRepository appUserRepository;
  private final DepartmentRepository departmentRepository;
  private final RoleRepository roleRepository;
  private final UserRoleRepository userRoleRepository;
  private final PasswordEncoder passwordEncoder;
  private final ConfidentialityRepository confidentialityRepository;
  private final UserAuditResolutionService userAuditResolutionService;
  private final EffectiveUserPermissionService effectiveUserPermissionService;
  private final PermissionRepository permissionRepository;

  @Transactional(readOnly = true)
  public Page<UserListDto> listUsers(Pageable pageable, String query) {
    String q = query == null ? "" : query.trim();
    Page<AppUserEntity> users = q.isEmpty()
        ? appUserRepository.findByDeletedAtIsNull(pageable)
        : appUserRepository.searchActiveDirectory(q, pageable);
    return users.map(UserListMapper::toListDto);
  }

  @Transactional(readOnly = true)
  public List<LookupItemDto> listRoles() {
    return roleRepository.findByDeletedAtIsNullOrderBySortOrderAsc().stream()
        .map(RoleMapper::toLookupItem)
        .toList();
  }

  @Transactional
  public void assignRole(UUID actorId, UUID targetUserId, Long roleId) {
    AppUserEntity user =
        appUserRepository
            .findByIdAndDeletedAtIsNull(targetUserId)
            .orElseThrow(() -> new NotFoundException("User not found"));
    if (!Boolean.TRUE.equals(user.getActive())) {
      throw new BadRequestException("Cannot assign roles to an inactive user");
    }
    roleRepository
        .findByIdAndDeletedAtIsNullAndActiveTrue(roleId)
        .orElseThrow(() -> new NotFoundException("RoleEntity not found"));

    UserRoleId id = new UserRoleId(targetUserId, roleId);
    Instant now = Instant.now();
    Optional<UserRoleEntity> existing = userRoleRepository.findById(id);
    if (existing.isPresent()) {
      UserRoleEntity ur = existing.get();
      if (ur.getValidTo() == null || ur.getValidTo().isAfter(now)) {
        return;
      }
      ur.setValidTo(null);
      ur.setValidFrom(now);
      ur.setUpdatedBy(actorId);
      userRoleRepository.save(ur);
      return;
    }

    UserRoleEntity ur = new UserRoleEntity();
    ur.setId(id);
    ur.setAppUser(appUserRepository.getReferenceById(targetUserId));
    ur.setRole(roleRepository.getReferenceById(roleId));
    ur.setValidFrom(now);
    ur.setValidTo(null);
    ur.setCreatedBy(actorId);
    ur.setUpdatedBy(actorId);
    userRoleRepository.save(ur);
  }

  @Transactional
  public void setRoles(UUID actorId, UUID targetUserId, List<Long> roleIds) {
    AppUserEntity user =
        appUserRepository
            .findByIdAndDeletedAtIsNull(targetUserId)
            .orElseThrow(() -> new NotFoundException("User not found"));
    if (!Boolean.TRUE.equals(user.getActive())) {
      throw new BadRequestException("Cannot assign roles to an inactive user");
    }

    Set<Long> selectedRoleIds =
        roleIds == null
            ? Set.of()
            : roleIds.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    if (selectedRoleIds.isEmpty()) {
      throw new BadRequestException("At least one role must be selected");
    }

    List<RoleEntity> validRoles = roleRepository.findByIdInAndDeletedAtIsNullAndActiveTrue(List.copyOf(selectedRoleIds));
    Set<Long> validRoleIds = validRoles.stream().map(RoleEntity::getId).collect(Collectors.toSet());
    if (validRoleIds.size() != selectedRoleIds.size()) {
      throw new NotFoundException("One or more roles were not found");
    }

    Instant now = Instant.now();
    List<UserRoleEntity> existing = userRoleRepository.findAllByUserId(targetUserId);
    Map<Long, UserRoleEntity> existingByRoleId =
        existing.stream().collect(Collectors.toMap(e -> e.getId().getRoleId(), Function.identity()));

    for (UserRoleEntity row : existing) {
      Long roleId = row.getId().getRoleId();
      boolean shouldBeActive = selectedRoleIds.contains(roleId);
      boolean isActive = row.getValidTo() == null || row.getValidTo().isAfter(now);

      if (shouldBeActive && !isActive) {
        row.setValidFrom(now);
        row.setValidTo(null);
        row.setUpdatedBy(actorId);
      } else if (!shouldBeActive && isActive) {
        row.setValidTo(now);
        row.setUpdatedBy(actorId);
      }
    }

    for (Long roleId : selectedRoleIds) {
      if (existingByRoleId.containsKey(roleId)) {
        continue;
      }
      UserRoleEntity ur = new UserRoleEntity();
      ur.setId(new UserRoleId(targetUserId, roleId));
      ur.setAppUser(user);
      ur.setRole(roleRepository.getReferenceById(roleId));
      ur.setValidFrom(now);
      ur.setValidTo(null);
      ur.setCreatedBy(actorId);
      ur.setUpdatedBy(actorId);
      userRoleRepository.save(ur);
    }
  }

  @Transactional(readOnly = true)
  public UserDetailDto getUserDetail(UUID userId) {
    AppUserEntity u =
        appUserRepository
            .findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));
    return detailResponse(u);
  }

  @Transactional
  public UserDetailDto createUser(UUID actorId, CreateAppUserRequestDto req) {
    String username = req.username().trim();
    if (appUserRepository.findByUsernameAndDeletedAtIsNull(username).isPresent()) {
      throw new BadRequestException("Username already exists");
    }
    DepartmentEntity dept =
        departmentRepository
            .findByIdAndDeletedAtIsNull(req.departmentId())
            .orElseThrow(() -> new NotFoundException("DepartmentEntity not found"));
    AppUserEntity u = new AppUserEntity();
    u.setUsername(username);
    u.setPasswordHash(passwordEncoder.encode(req.password()));
    u.setFullNameAr(req.fullNameAr().trim());
    u.setFullNameEn(req.fullNameEn().trim());
    u.setEmail(req.email().trim().toLowerCase());
    u.setDepartment(dept);
    u.setActive(true);
    u.setFailedLoginCount(0);
    u.setMustChangePassword(true);
    u.setSecurityClearanceId(resolveClearanceId(req.securityClearanceId()));
    u.setCreatedBy(actorId);
    u.setUpdatedBy(actorId);
    appUserRepository.save(u);
    return detailResponse(u);
  }

  @Transactional
  public UserDetailDto updateUser(UUID actorId, UUID userId, UpdateAppUserRequestDto req) {
    AppUserEntity u =
        appUserRepository
            .findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));
    DepartmentEntity dept =
        departmentRepository
            .findByIdAndDeletedAtIsNull(req.departmentId())
            .orElseThrow(() -> new NotFoundException("DepartmentEntity not found"));
    u.setFullNameAr(req.fullNameAr().trim());
    u.setFullNameEn(req.fullNameEn().trim());
    u.setEmail(req.email().trim().toLowerCase());
    u.setDepartment(dept);
    u.setActive(req.active());
    if (req.securityClearanceId() != null) {
      u.setSecurityClearanceId(resolveClearanceId(req.securityClearanceId()));
    }
    if (req.password() != null && !req.password().isBlank()) {
      u.setPasswordHash(passwordEncoder.encode(req.password().trim()));
      u.setMustChangePassword(true);
    }
    u.setUpdatedBy(actorId);
    appUserRepository.save(u);
    return detailResponse(u);
  }

  @Transactional
  public UserDetailDto toggleActive(UUID actorId, UUID userId) {
    AppUserEntity u =
        appUserRepository
            .findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));
    u.setActive(!Boolean.TRUE.equals(u.getActive()));
    u.setUpdatedBy(actorId);
    appUserRepository.save(u);
    return detailResponse(u);
  }

  @Transactional
  public void deleteUser(UUID actorId, UUID userId) {
    AppUserEntity u =
        appUserRepository
            .findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));
    u.setDeletedAt(Instant.now());
    u.setDeletedBy(actorId);
    u.setUpdatedBy(actorId);
    appUserRepository.save(u);
  }

  private UserDetailDto detailResponse(AppUserEntity user) {
    List<Long> roleIds = userRoleRepository.findActiveRoleIdsByUserId(user.getId());
    UserAuditRefDto createdByUser =
        userAuditResolutionService.toRef(user.getCreatedBy()).orElse(null);
    UserAuditRefDto updatedByUser =
        userAuditResolutionService.toRef(user.getUpdatedBy()).orElse(null);
    return UserAdminMapper.toDetailDto(user, roleIds, createdByUser, updatedByUser);
  }

  private Long resolveClearanceId(Long clearanceId) {
    if (clearanceId == null) {
      return null;
    }
    confidentialityRepository
        .findByIdAndDeletedAtIsNull(clearanceId)
        .orElseThrow(() -> new NotFoundException("Security clearance not found"));
    return clearanceId;
  }

  @Transactional(readOnly = true)
  public List<EffectivePermissionDto> effectivePermissions(UUID userId) {
    if (!appUserRepository.existsById(userId)) {
      throw new NotFoundException("User not found");
    }
    var ids = effectiveUserPermissionService.effectivePermissionIds(userId);
    return permissionRepository.findByDeletedAtIsNullOrderBySortOrderAsc().stream()
        .filter(permission -> ids.contains(permission.getId()))
        .map(permission -> new EffectivePermissionDto(
            permission.getId(), permission.getCode(), permission.getNameAr(), permission.getNameEn()))
        .toList();
  }
}
