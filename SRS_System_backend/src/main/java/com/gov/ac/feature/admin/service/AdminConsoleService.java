package com.gov.ac.feature.admin.service;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.domain.admin.UiScreen;
import com.gov.ac.domain.user.Permission;
import com.gov.ac.domain.user.Role;
import com.gov.ac.domain.user.RolePermission;
import com.gov.ac.domain.user.RolePermissionId;
import com.gov.ac.feature.admin.dto.PermissionDto;
import com.gov.ac.feature.admin.dto.UiScreenDto;
import com.gov.ac.feature.admin.dto.UpsertPermissionRequest;
import com.gov.ac.feature.admin.dto.UpsertUiScreenRequest;
import com.gov.ac.persistence.PermissionRepository;
import com.gov.ac.persistence.RolePermissionRepository;
import com.gov.ac.persistence.RoleRepository;
import com.gov.ac.persistence.UiScreenRepository;
import com.gov.ac.security.SecurityUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminConsoleService {

  private final PermissionRepository permissionRepository;
  private final RoleRepository roleRepository;
  private final RolePermissionRepository rolePermissionRepository;
  private final UiScreenRepository uiScreenRepository;

  @Transactional(readOnly = true)
  public List<PermissionDto> listPermissions() {
    return permissionRepository.findByDeletedAtIsNullOrderBySortOrderAsc().stream()
        .map(AdminConsoleService::toPermissionDto)
        .toList();
  }

  @Transactional
  public PermissionDto createPermission(UpsertPermissionRequest req) {
    String code = req.code().trim();
    if (permissionRepository.existsByCodeIgnoreCaseAndDeletedAtIsNull(code)) {
      throw new BadRequestException("Permission code already exists");
    }
    UUID actor = SecurityUtils.requireCurrentUserId();
    Permission p = new Permission();
    p.setCode(code);
    p.setNameAr(req.nameAr().trim());
    p.setNameEn(req.nameEn().trim());
    p.setDescription(trimToNull(req.description()));
    p.setSortOrder(req.sortOrder());
    p.setActive(req.active());
    p.setUiScreenId(resolveUiScreenIdOrNull(req.uiScreenId()));
    p.setCreatedBy(actor);
    p.setUpdatedBy(actor);
    return toPermissionDto(permissionRepository.save(p));
  }

  @Transactional
  public PermissionDto updatePermission(Long id, UpsertPermissionRequest req) {
    Permission p =
        permissionRepository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Permission not found"));
    String code = req.code().trim();
    if (permissionRepository.existsByCodeIgnoreCaseAndDeletedAtIsNullAndIdNot(code, id)) {
      throw new BadRequestException("Permission code already exists");
    }
    UUID actor = SecurityUtils.requireCurrentUserId();
    p.setCode(code);
    p.setNameAr(req.nameAr().trim());
    p.setNameEn(req.nameEn().trim());
    p.setDescription(trimToNull(req.description()));
    p.setSortOrder(req.sortOrder());
    p.setActive(req.active());
    p.setUiScreenId(resolveUiScreenIdOrNull(req.uiScreenId()));
    p.setUpdatedBy(actor);
    return toPermissionDto(permissionRepository.save(p));
  }

  @Transactional
  public void deletePermission(Long id) {
    Permission p =
        permissionRepository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Permission not found"));
    UUID actor = SecurityUtils.requireCurrentUserId();
    p.setDeletedAt(java.time.Instant.now());
    p.setDeletedBy(actor);
    p.setUpdatedBy(actor);
    permissionRepository.save(p);
  }

  @Transactional(readOnly = true)
  public List<Long> listPermissionIdsForRole(Long roleId) {
    ensureRole(roleId);
    return rolePermissionRepository.findPermissionIdsByRoleId(roleId);
  }

  @Transactional
  public void replaceRolePermissions(Long roleId, List<Long> permissionIds) {
    Role role = ensureRole(roleId);
    SecurityUtils.requireCurrentUserId();
    rolePermissionRepository.deleteByRoleId(roleId);
    if (permissionIds == null || permissionIds.isEmpty()) {
      return;
    }
    List<RolePermission> rows = new ArrayList<>();
    for (Long pid : permissionIds.stream().distinct().toList()) {
      permissionRepository
          .findByIdAndDeletedAtIsNull(pid)
          .orElseThrow(() -> new NotFoundException("Permission not found: " + pid));
      RolePermission rp = new RolePermission();
      rp.setId(new RolePermissionId(role.getId(), pid));
      rows.add(rp);
    }
    rolePermissionRepository.saveAll(rows);
  }

  @Transactional(readOnly = true)
  public List<UiScreenDto> listUiScreens() {
    return uiScreenRepository.findByDeletedAtIsNullOrderBySortOrderAsc().stream()
        .map(AdminConsoleService::toUiScreenDto)
        .toList();
  }

  @Transactional
  public UiScreenDto createUiScreen(UpsertUiScreenRequest req) {
    String code = req.code().trim();
    if (uiScreenRepository.existsByCodeIgnoreCaseAndDeletedAtIsNull(code)) {
      throw new BadRequestException("Screen code already exists");
    }
    UUID actor = SecurityUtils.requireCurrentUserId();
    UiScreen s = new UiScreen();
    s.setCode(code);
    s.setRoutePath(req.routePath().trim());
    s.setNameAr(req.nameAr().trim());
    s.setNameEn(req.nameEn().trim());
    s.setDescription(trimToNull(req.description()));
    s.setSortOrder(req.sortOrder());
    s.setActive(req.active());
    s.setIconKey(normalizeIconKey(req.iconKey()));
    s.setShowInShellNav(Boolean.TRUE.equals(req.showInShellNav()));
    s.setRequiredPermissionId(req.requiredPermissionId());
    s.setCreatedBy(actor);
    s.setUpdatedBy(actor);
    return toUiScreenDto(uiScreenRepository.save(s));
  }

  @Transactional
  public UiScreenDto updateUiScreen(Long id, UpsertUiScreenRequest req) {
    UiScreen s =
        uiScreenRepository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Screen not found"));
    String code = req.code().trim();
    if (uiScreenRepository.existsByCodeIgnoreCaseAndDeletedAtIsNullAndIdNot(code, id)) {
      throw new BadRequestException("Screen code already exists");
    }
    UUID actor = SecurityUtils.requireCurrentUserId();
    s.setCode(code);
    s.setRoutePath(req.routePath().trim());
    s.setNameAr(req.nameAr().trim());
    s.setNameEn(req.nameEn().trim());
    s.setDescription(trimToNull(req.description()));
    s.setSortOrder(req.sortOrder());
    s.setActive(req.active());
    s.setIconKey(normalizeIconKey(req.iconKey()));
    s.setShowInShellNav(Boolean.TRUE.equals(req.showInShellNav()));
    s.setRequiredPermissionId(req.requiredPermissionId());
    s.setUpdatedBy(actor);
    return toUiScreenDto(uiScreenRepository.save(s));
  }

  @Transactional
  public void deleteUiScreen(Long id) {
    UiScreen s =
        uiScreenRepository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Screen not found"));
    UUID actor = SecurityUtils.requireCurrentUserId();
    s.setDeletedAt(java.time.Instant.now());
    s.setDeletedBy(actor);
    s.setUpdatedBy(actor);
    uiScreenRepository.save(s);
  }

  private Role ensureRole(Long roleId) {
    return roleRepository
        .findByIdAndDeletedAtIsNullAndActiveTrue(roleId)
        .orElseThrow(() -> new NotFoundException("Role not found"));
  }

  private Long resolveUiScreenIdOrNull(Long id) {
    if (id == null) {
      return null;
    }
    return uiScreenRepository
        .findByIdAndDeletedAtIsNull(id)
        .map(UiScreen::getId)
        .orElseThrow(() -> new NotFoundException("UI screen not found"));
  }

  private static PermissionDto toPermissionDto(Permission p) {
    return new PermissionDto(
        p.getId(),
        p.getCode(),
        p.getNameAr(),
        p.getNameEn(),
        p.getDescription(),
        p.getSortOrder(),
        p.getActive(),
        p.getUiScreenId());
  }

  private static UiScreenDto toUiScreenDto(UiScreen s) {
    return new UiScreenDto(
        s.getId(),
        s.getCode(),
        s.getRoutePath(),
        s.getNameAr(),
        s.getNameEn(),
        s.getDescription(),
        s.getSortOrder(),
        s.getActive(),
        s.getRequiredPermissionId(),
        s.getIconKey(),
        s.getShowInShellNav());
  }

  private static String normalizeIconKey(String raw) {
    if (!StringUtils.hasText(raw)) {
      return "apps";
    }
    return raw.trim();
  }

  private static String trimToNull(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }
}
