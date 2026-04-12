package com.gov.ac.feature.users.service;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.domain.org.Department;
import com.gov.ac.domain.user.AppUser;
import com.gov.ac.domain.user.Role;
import com.gov.ac.domain.user.UserRole;
import com.gov.ac.domain.user.UserRoleId;
import com.gov.ac.feature.lookups.dto.LookupItemDto;
import com.gov.ac.feature.users.dto.CreateAppUserRequest;
import com.gov.ac.feature.users.dto.UpdateAppUserRequest;
import com.gov.ac.feature.users.dto.UserDetailDto;
import com.gov.ac.feature.users.dto.UserListDto;
import com.gov.ac.feature.users.mapper.UserListMapper;
import com.gov.ac.persistence.AppUserRepository;
import com.gov.ac.persistence.DepartmentRepository;
import com.gov.ac.persistence.RoleRepository;
import com.gov.ac.persistence.UserRoleRepository;
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
                    r.getId(), r.getCode(), r.getNameAr(), r.getNameEn(), r.getSortOrder(), null))
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

  @Transactional
  public void setRoles(UUID actorId, UUID targetUserId, List<Long> roleIds) {
    AppUser user =
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

    List<Role> validRoles = roleRepository.findByIdInAndDeletedAtIsNullAndActiveTrue(List.copyOf(selectedRoleIds));
    Set<Long> validRoleIds = validRoles.stream().map(Role::getId).collect(Collectors.toSet());
    if (validRoleIds.size() != selectedRoleIds.size()) {
      throw new NotFoundException("One or more roles were not found");
    }

    Instant now = Instant.now();
    List<UserRole> existing = userRoleRepository.findAllByUserId(targetUserId);
    Map<Long, UserRole> existingByRoleId =
        existing.stream().collect(Collectors.toMap(e -> e.getId().getRoleId(), Function.identity()));

    for (UserRole row : existing) {
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
      UserRole ur = new UserRole();
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
    AppUser u =
        appUserRepository
            .findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));
    return toDetailDto(u);
  }

  @Transactional
  public UserDetailDto createUser(UUID actorId, CreateAppUserRequest req) {
    String username = req.username().trim();
    if (appUserRepository.findByUsernameAndDeletedAtIsNull(username).isPresent()) {
      throw new BadRequestException("Username already exists");
    }
    Department dept =
        departmentRepository
            .findByIdAndDeletedAtIsNull(req.departmentId())
            .orElseThrow(() -> new NotFoundException("Department not found"));
    AppUser u = new AppUser();
    u.setUsername(username);
    u.setPasswordHash(passwordEncoder.encode(req.password()));
    u.setFullNameAr(req.fullNameAr().trim());
    u.setFullNameEn(req.fullNameEn().trim());
    u.setEmail(req.email().trim().toLowerCase());
    u.setDepartment(dept);
    u.setActive(true);
    u.setFailedLoginCount(0);
    u.setCreatedBy(actorId);
    u.setUpdatedBy(actorId);
    appUserRepository.save(u);
    return toDetailDto(u);
  }

  @Transactional
  public UserDetailDto updateUser(UUID actorId, UUID userId, UpdateAppUserRequest req) {
    AppUser u =
        appUserRepository
            .findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));
    Department dept =
        departmentRepository
            .findByIdAndDeletedAtIsNull(req.departmentId())
            .orElseThrow(() -> new NotFoundException("Department not found"));
    u.setFullNameAr(req.fullNameAr().trim());
    u.setFullNameEn(req.fullNameEn().trim());
    u.setEmail(req.email().trim().toLowerCase());
    u.setDepartment(dept);
    u.setActive(req.active());
    if (req.password() != null && !req.password().isBlank()) {
      u.setPasswordHash(passwordEncoder.encode(req.password().trim()));
    }
    u.setUpdatedBy(actorId);
    appUserRepository.save(u);
    return toDetailDto(u);
  }

  @Transactional
  public void deleteUser(UUID actorId, UUID userId) {
    AppUser u =
        appUserRepository
            .findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));
    u.setDeletedAt(Instant.now());
    u.setDeletedBy(actorId);
    u.setUpdatedBy(actorId);
    appUserRepository.save(u);
  }

  private UserDetailDto toDetailDto(AppUser u) {
    Department d = u.getDepartment();
    List<Long> roleIds = userRoleRepository.findActiveRoleIdsByUserId(u.getId());
    return new UserDetailDto(
        u.getId(),
        u.getUsername(),
        u.getFullNameAr(),
        u.getFullNameEn(),
        u.getEmail(),
        d != null ? d.getCode() : null,
        d != null ? d.getId() : null,
        u.getActive(),
        roleIds);
  }
}
